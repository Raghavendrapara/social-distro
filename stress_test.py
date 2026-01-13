import requests
import concurrent.futures
import time
import uuid
import random

# CONFIGURATION
BASE_URL = "http://localhost:8080"
NUM_ITEMS = 100          # Number of data items to push
CONCURRENCY = 20         # Number of concurrent threads pushing data
POLL_INTERVAL = 1        # Seconds to wait between status checks

def create_pod():
    """Creates a fresh pod for the test."""
    payload = {
        "name": f"Stress Test {uuid.uuid4().hex[:6]}",
        "ownerUserId": "stress-tester"
    }
    try:
        resp = requests.post(f"{BASE_URL}/pods", json=payload)
        resp.raise_for_status()
        pod_id = resp.json()['podId']
        print(f"✅ Created Pod: {pod_id}")
        return pod_id
    except Exception as e:
        print(f"❌ Failed to create pod: {e}")
        exit(1)

def add_data(pod_id, index):
    """Sends a single data item to the pod."""
    content = f"Item {index}: This is some random text data to generate embeddings. {uuid.uuid4()}"
    try:
        resp = requests.post(f"{BASE_URL}/pods/{pod_id}/data", json={"content": content})
        resp.raise_for_status()
        return True
    except Exception as e:
        print(f"⚠️ Failed to add item {index}: {e}")
        return False

def start_indexing(pod_id):
    """Triggers the indexing job."""
    try:
        resp = requests.post(f"{BASE_URL}/indexing/pods/{pod_id}")
        resp.raise_for_status()
        job_id = resp.json()['jobId']
        print(f"🚀 Indexing Triggered. Job ID: {job_id}")
        return job_id
    except Exception as e:
        print(f"❌ Failed to start indexing: {e}")
        exit(1)

def wait_for_job(job_id):
    """Polls the job status until completion."""
    start_time = time.time()
    while True:
        try:
            resp = requests.get(f"{BASE_URL}/indexing/jobs/{job_id}")
            resp.raise_for_status()
            data = resp.json()
            status = data['status']

            elapsed = time.time() - start_time
            print(f"⏳ Job Status: {status} (Elapsed: {elapsed:.2f}s)", end='\r')

            if status == "COMPLETED":
                print(f"\n✅ Job COMPLETED in {elapsed:.2f} seconds!")
                return True
            elif status == "FAILED":
                print(f"\n❌ Job FAILED: {data.get('errorMessage')}")
                return False

            time.sleep(POLL_INTERVAL)
        except Exception as e:
            print(f"\n⚠️ Error polling job: {e}")
            time.sleep(POLL_INTERVAL)

def main():
    print(f"🔥 Starting Stress Test: {NUM_ITEMS} items, {CONCURRENCY} threads\n")

    # 1. Create Pod
    pod_id = create_pod()

    # 2. Flood Data (Concurrent Pushes)
    print(f"🌊 Flooding {NUM_ITEMS} items...")
    start_load = time.time()

    with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENCY) as executor:
        futures = [executor.submit(add_data, pod_id, i) for i in range(NUM_ITEMS)]
        completed = 0
        for f in concurrent.futures.as_completed(futures):
            if f.result():
                completed += 1

    duration_load = time.time() - start_load
    print(f"✅ Data Load Finished: {completed}/{NUM_ITEMS} items in {duration_load:.2f}s")

    # 3. Start Indexing (The Real System Test)
    job_id = start_indexing(pod_id)

    # 4. Wait for Completion
    wait_for_job(job_id)

    # 5. Verify Metrics (Optional)
    try:
        # Check if vectors actually exist by asking a question
        query_payload = {"question": "random text data"}
        q_resp = requests.post(f"{BASE_URL}/pods/{pod_id}/query", json=query_payload)
        if q_resp.status_code == 200:
             print(f"\n💡 Query Test: System answered successfully.")
    except:
        pass

if __name__ == "__main__":
    main()