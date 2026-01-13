# API Reference (HTTPie Examples)

Quick reference for all API endpoints using [HTTPie](https://httpie.io/).

---

## Prerequisites
```bash
# Install HTTPie
pip install httpie

# Start the application
docker compose up -d --build
```

---

## Pod Management

### Create a Pod
```bash
http POST localhost:8080/pods \
  name="My Knowledge Base" \
  ownerUserId="user123"
```
**Response:**
```json
{
  "podId": "abc-123-def",
  "name": "My Knowledge Base",
  "ownerUserId": "user123"
}
```

### Get Pod Details
```bash
http GET localhost:8080/pods/{podId}
```

### Add Data to Pod
```bash
http POST localhost:8080/pods/{podId}/data \
  content="This is my document content that will be indexed and searchable."
```

---

## Indexing

### Start Indexing Job
```bash
http POST localhost:8080/indexing/pods/{podId}
```
**Response:**
```json
{
  "jobId": "job-456-xyz"
}
```

### Check Job Status
```bash
http GET localhost:8080/indexing/jobs/{jobId}
```
**Response:**
```json
{
  "jobId": "job-456-xyz",
  "status": "COMPLETED",
  "startedAt": "2026-01-13T18:00:00Z",
  "finishedAt": "2026-01-13T18:00:05Z"
}
```

---

## Query (RAG)

### Ask a Question
```bash
http POST localhost:8080/pods/{podId}/query \
  question="What does my document say about X?"
```
**Response:**
```json
{
  "answer": "Based on your documents, X is..."
}
```

### Bypass Cache (Force Fresh LLM Call)
```bash
http POST localhost:8080/pods/{podId}/query \
  question="What is X?" \
  X-Cache-Bypass:true
```

---

## Health & Metrics

### Health Check
```bash
http GET localhost:8080/actuator/health
```

### Prometheus Metrics
```bash
http GET localhost:8080/actuator/prometheus
```

### Application Info
```bash
http GET localhost:8080/actuator/info
```

---

## Complete Workflow Example
```bash
# 1. Create a pod
POD_ID=$(http POST localhost:8080/pods name="Test Pod" ownerUserId="user1" | jq -r '.podId')

# 2. Add data
http POST localhost:8080/pods/$POD_ID/data content="Machine learning is a subset of AI."
http POST localhost:8080/pods/$POD_ID/data content="Deep learning uses neural networks."
http POST localhost:8080/pods/$POD_ID/data content="GPT is a large language model."

# 3. Start indexing
JOB_ID=$(http POST localhost:8080/indexing/pods/$POD_ID | jq -r '.jobId')

# 4. Wait for completion
while [ "$(http GET localhost:8080/indexing/jobs/$JOB_ID | jq -r '.status')" = "RUNNING" ]; do
  sleep 2
done

# 5. Query
http POST localhost:8080/pods/$POD_ID/query question="What is deep learning?"
```
