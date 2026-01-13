import requests
import time
import subprocess
import concurrent.futures
import uuid
import sys

BASE_URL = "http://localhost:8080"
POD_NAME = f"Chaos Test Pod {uuid.uuid4()}"
USER_ID = "chaos-user"
LLM_CONTAINER = "social-distro-llm"

def create_and_seed_pod():
    print(f"Creating Pod: {POD_NAME}...")
    resp = requests.post(f"{BASE_URL}/pods", json={"name": POD_NAME, "ownerUserId": USER_ID})
    resp.raise_for_status()
    pod_id = resp.json()["podId"]
    
    # Add some data to index
    requests.post(f"{BASE_URL}/pods/{pod_id}/data", json={"content": "Resilience testing is crucial for distributed systems."})
    resp = requests.post(f"{BASE_URL}/indexing/pods/{pod_id}")
    resp.raise_for_status()
    job_id = resp.json()["jobId"]
    wait_for_job(job_id)
    
    return pod_id

def wait_for_job(job_id):
    print("   Waiting for indexing...")
    while True:
        resp = requests.get(f"{BASE_URL}/indexing/jobs/{job_id}")
        status = resp.json()["status"]
        if status == "COMPLETED":
            break
        elif status == "FAILED":
            raise Exception("Indexing Failed")
        time.sleep(1)

def ensure_container_running():
    print(f"Ensuring {LLM_CONTAINER} is running...")
    subprocess.run(["docker", "start", LLM_CONTAINER], check=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(5) # Wait for startup

def warmup_pod(pod_id):
    print("WARMUP: Sending initial request to load model...")
    # Retry up to 3 times
    for i in range(3):
        try:
            print(f"   Warmup Attempt {i+1}...")
            resp = requests.post(f"{BASE_URL}/pods/{pod_id}/query", json={"question": "Warmup?"}, timeout=120)
            if resp.status_code == 200:
                print("   Warmup Successful.")
                return
        except Exception as e:
            print(f"   Warmup Attempt {i+1} failed: {e}")
            time.sleep(2)
    print("   Warmup finished (may have failed).")

def send_query(pod_id, expect_fallback=False):
    try:
        start = time.time()
        # Randomized query to bypass cache for resilience testing
        query = f"Why is resilience important? {uuid.uuid4()}"
        
        # increased timeout to 60s for local LLM
        resp = requests.post(f"{BASE_URL}/pods/{pod_id}/query", json={"question": query}, timeout=60)
        duration = time.time() - start
        
        if resp.status_code == 200:
            data = resp.json()
            answer = data.get("answer", "")
            
            if "Comparison temporarily unavailable" in answer:
                if expect_fallback:
                    print(f"✅ Fallback received as expected ({duration:.2f}s)")
                    return True
                else:
                    print(f"⚠️ Unexpected Fallback received ({duration:.2f}s)")
                    return False
            else:
                if expect_fallback:
                    print(f"❌ Real response received but expected fallback ({duration:.2f}s)")
                    return False
                else:
                    print(f"✅ Real response received ({duration:.2f}s)")
                    return True
        else:
            print(f"❌ Error: {resp.status_code} ({duration:.2f}s)")
            return False
            
    except Exception as e:
        print(f"❌ Request Failed: {e}")
        return False

def toggle_container(container_name, action):
    print(f"\n🐋 Docker: {action} {container_name}...")
    subprocess.run(["docker", action, container_name], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    print(f"   Done.")

def test_circuit_breaker(pod_id):
    print("\n--- TEST: Circuit Breaker Resilience ---")
    
    print("1. Verifying normal operation...")
    if not send_query(pod_id, expect_fallback=False):
        print("Skipping remaining tests due to initial failure.")
        return

    print("2. Simulating LLM Failure (Stopping Container)...")
    toggle_container(LLM_CONTAINER, "stop")
    time.sleep(2) # Allow checking to detect failure

    print("3. Verifying Fallback Response...")
    # It might take a few calls to open the circuit breaker
    fallback_seen = False
    for i in range(10):
        print(f"   Query {i+1}: ", end="")
        if send_query(pod_id, expect_fallback=True):
            fallback_seen = True
            break
        time.sleep(0.5)
        
    if fallback_seen:
        print("✅ Circuit Breaker OPENED. Fallback is working.")
    else:
        print("❌ Circuit Breaker failed to open or fallback not triggered.")

    print("4. Recovering System (Starting Container)...")
    toggle_container(LLM_CONTAINER, "start")
    print("   Waiting for container to be ready (10s)...")
    time.sleep(10) # Wait for Ollama to boot

    print("5. Verifying Recovery...")
    recovered = False
    for i in range(10):
        print(f"   Query {i+1}: ", end="")
        if send_query(pod_id, expect_fallback=False):
            recovered = True
            break
        time.sleep(2)
        
    if recovered:
        print("✅ Circuit Breaker CLOSED. System recovered.")
    else:
        print("❌ System failed to recover.")

def test_concurrency(pod_id):
    print("\n--- TEST: High Concurrency (50 threads) ---")
    with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
        futures = [executor.submit(send_query, pod_id, False) for _ in range(50)]
        results = [f.result() for f in concurrent.futures.as_completed(futures)]
        
    success_count = results.count(True)
    print(f"Success Rate: {success_count}/50")

if __name__ == "__main__":
    try:
        pod_id = create_and_seed_pod()
        test_circuit_breaker(pod_id)
        test_concurrency(pod_id)
    except Exception as e:
        print(f"\nCRITICAL ERROR: {e}")
        # Ensure container is back up even if test crashes
        try:
            subprocess.run(["docker", "start", LLM_CONTAINER])
        except:
            pass
