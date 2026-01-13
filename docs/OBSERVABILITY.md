# Observability Guide: Prometheus, Grafana & Zipkin

How to use the monitoring tools to understand your application's behavior.

---

## Quick Access URLs
| Tool | URL | Purpose |
|---|---|---|
| **Grafana** | http://localhost:3000 | Dashboards & Visualization |
| **Prometheus** | http://localhost:9090 | Metrics Database & Queries |
| **Zipkin** | http://localhost:9411 | Distributed Tracing |
| **Actuator** | http://localhost:8080/actuator | App Health & Metrics |

**Grafana Credentials:** admin / admin

---

## Critical Setup Notes

> [!IMPORTANT]
> **Grafana Data Source URL**: When adding Prometheus as a data source in Grafana, use `http://prometheus:9090` (Docker DNS name), NOT `http://localhost:9090`.

> [!IMPORTANT]
> **Application Label**: Metrics are tagged with `application=social-distro`. Select this in dashboard dropdowns.

---

## 1. Prometheus: Querying Metrics

### Access
Open http://localhost:9090 in your browser.

### Basic Queries (Type in the query box)

| Metric | Query | What It Shows |
|---|---|---|
| HTTP request rate | `rate(http_server_requests_seconds_count[5m])` | Requests per second |
| Response time P95 | `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))` | 95th percentile latency |
| JVM memory | `jvm_memory_used_bytes{area="heap"}` | Heap memory usage |
| Circuit breaker state | `resilience4j_circuitbreaker_state` | 0=closed, 1=open, 2=half_open |
| Indexing throughput | `rate(indexing_item_process_seconds_count[5m])` | Items indexed/sec |

### How to Use
1. Go to http://localhost:9090/graph
2. Paste a query
3. Click "Execute"
4. Switch to "Graph" tab to see time series

---

## 2. Grafana: Building Dashboards

### First Login
1. Open http://localhost:3000
2. Login: admin / admin
3. Skip password change (or set a new one)

### Import Pre-built Dashboards
1. Click **+** → **Import**
2. Enter Dashboard ID → **Load** → **Import**

| Dashboard | ID | Description |
|---|---|---|
| JVM Micrometer | 4701 | Memory, GC, threads |
| Spring Boot | 12900 | HTTP metrics |
| Kafka Exporter | 7589 | Consumer lag, throughput |
| Redis | 763 | Operations, memory |

### Create Custom Dashboard
1. Click **+** → **New Dashboard**
2. **Add Visualization**
3. Select **Prometheus** data source
4. Enter query: `rate(http_server_requests_seconds_count[1m])`
5. Click **Apply**

### Key Panels to Create

**Request Rate:**
- Query: `sum(rate(http_server_requests_seconds_count{uri!~"/actuator.*"}[5m]))`
- Type: Time series

**Error Rate:**
- Query: `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))`
- Type: Stat

**JVM Heap:**
- Query: `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100`
- Type: Gauge

---

## 3. Zipkin: Tracing Requests

### Access
Open http://localhost:9411 in your browser.

### Finding Traces
1. Click **Run Query** (top right)
2. See list of recent traces
3. Click any trace to see details

### Filtering Traces
| Filter | How |
|---|---|
| By service | Select "social-distro" from dropdown |
| By time | Set "Start Time" and duration |
| By endpoint | Add tag: `http.path=/pods/{id}/query` |
| Errors only | Add tag: `error=true` |

### Reading a Trace
```
[Query Request] ──┬── [EmbeddingClient.embed]
                  │
                  ├── [VectorStore.findSimilar]
                  │
                  └── [LlmClient.generateAnswer] ← Usually slowest
```

Each row shows:
- **Service name**: Which component
- **Duration**: Time taken (hover for details)
- **Annotations**: Start/end timestamps

### Common Issues to Spot
| Pattern | Indicates |
|---|---|
| Long LLM span | Model inference is slow |
| Missing spans | Instrumentation gap |
| Many retries | Unstable downstream |
| Error tags | Failed operations |

---

## 4. Common Debugging Scenarios

### "API is slow" → Check Zipkin
1. Find slow trace
2. Identify longest span
3. Usually LLM or database

### "Errors increasing" → Check Prometheus
```
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (uri)
```

### "Memory growing" → Check Grafana JVM Dashboard
- Look at heap usage over time
- Check GC pause frequency
- GC not reclaiming memory = leak

### "Indexing stuck" → Check Kafka lag
```
# In terminal
docker exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group social-distro-workers-v3 \
  --describe
```

---

## 5. Alerting (Future Setup)

Add to `monitoring/alerting_rules.yml`:
```yaml
groups:
  - name: social-distro
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
```

Then configure Grafana: **Alerting** → **Contact Points** → Add email/Slack.
