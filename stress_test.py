import requests
import time
import uuid

BASE_URL = "http://localhost:8080"
POD_NAME = f"Stress Test Pod {uuid.uuid4()}"
USER_ID = "stress-user"
NUM_ITEMS = 50

def create_pod():
    print(f"Creating Pod: {POD_NAME}...")
    resp = requests.post(f"{BASE_URL}/pods", json={"name": POD_NAME, "ownerUserId": USER_ID})
    resp.raise_for_status()
    pod_id = resp.json()["podId"]
    print(f"Pod Created: {pod_id}")
    return pod_id

def add_items(pod_id):
    print(f"Adding {NUM_ITEMS} items...")
    for i in range(NUM_ITEMS):
        content = f"This is stress test item number {i} for distributed system testing."
        requests.post(f"{BASE_URL}/pods/{pod_id}/data", json={"content": content})
    print("All items added.")

def trigger_indexing(pod_id):
    print("Triggering Indexing Job...")
    resp = requests.post(f"{BASE_URL}/indexing/pods/{pod_id}")
    resp.raise_for_status()
    job_id = resp.json()["jobId"]
    print(f"Job Triggered: {job_id}")
    return job_id

def wait_for_job(job_id):
    print("Waiting for job completion...")
    while True:
        resp = requests.get(f"{BASE_URL}/indexing/jobs/{job_id}")
        status = resp.json()["status"]
        if status == "COMPLETED":
            print("Job COMPLETED!")
            break
        elif status == "FAILED":
            print(f"Job FAILED: {resp.json()}")
            break
        print(f"Current Status: {status}...")
        time.sleep(1)

if __name__ == "__main__":
    try:
        pod_id = create_pod()
        add_items(pod_id)
        job_id = trigger_indexing(pod_id)
        wait_for_job(job_id)
        
        print("\n--- TEST PASSED ---")
        print("Now check observability:")
        print("1. Metrics: http://localhost:8080/actuator/prometheus")
        print("2. Search for: indexing_item_process_seconds_count")
    except Exception as e:
        print(f"\n--- TEST FAILED: {e} ---")
        import traceback
        traceback.print_exc()
