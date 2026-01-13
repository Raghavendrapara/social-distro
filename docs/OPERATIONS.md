# Operations & Production Readiness Guide

---

## 1. Monitoring Setup

### Current State
The application exposes metrics at:
```
http://localhost:8080/actuator/prometheus
```

### Observability Stack (Already Configured)

The following services are in `docker-compose.yml`:
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **Zipkin**: http://localhost:9411

### Prometheus Configuration (`monitoring/prometheus.yml`)

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'social-distro'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['social-distro-backend:8080']
```

> **Important**: Prometheus must be on the same Docker network (`datahub-network`) as the app to resolve the container name.

### Grafana Data Source Setup
1. Go to **Connections** → **Data sources** → **Add Prometheus**
2. URL: `http://prometheus:9090` (NOT localhost!)
3. Click **Save & Test**

### Recommended Dashboards (Import by ID)
| Dashboard | ID | Purpose |
|---|---|---|
| Spring Boot APM | 12900 | HTTP metrics, JVM, HikariCP |
| JVM Micrometer | 4701 | Memory, GC, threads |

### Dashboard Variables
After importing, set these in the dashboard:
- **application**: `social-distro`
- **instance**: `social-distro-backend:8080`

---

## 1.5 Distributed Tracing (Zipkin)

The application already has Zipkin dependencies configured. Add to `docker-compose.yml`:

```yaml
  zipkin:
    image: openzipkin/zipkin:3.4
    container_name: zipkin
    ports:
      - "9411:9411"
    environment:
      - STORAGE_TYPE=mem
```

Add to `application.yaml`:
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # Sample 100% of requests (reduce in prod)
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

### Access Zipkin
| Service | URL |
|---|---|
| Zipkin UI | http://localhost:9411 |

### What You'll See
- **Trace ID**: Follows a request across services
- **Spans**: Each service/operation in the trace
- **Latency breakdown**: Time spent in each component

### Useful Traces to Analyze
| Flow | What to Look For |
|---|---|
| Query → Embedding → LLM | LLM latency bottleneck |
| Indexing → Kafka → Worker | Async handoff timing |
| Any 5xx error | Stack trace in span tags |

### JVM Metrics to Watch
```
jvm_memory_used_bytes{area="heap"}
jvm_gc_pause_seconds_count
jvm_threads_live_threads
```

### Common Leak Patterns

| Issue | Symptom | Fix |
|---|---|---|
| Unclosed streams | OOM after hours | Ensure `try-with-resources` |
| Event listener not removed | Growing thread count | Unregister on shutdown |
| Cache without eviction | Heap grows forever | Set TTL in Redis config |
| Kafka consumer lag | Memory pressure | Scale consumers |

### Tools for Detection
```bash
# Heap dump
docker exec social-distro-backend jcmd 1 GC.heap_dump /tmp/heap.hprof

# Analyze with
# - Eclipse MAT
# - VisualVM
# - JProfiler
```

### Application Config for Leak Prevention
Add to `application.yaml`:
```yaml
spring:
  data:
    redis:
      cache:
        time-to-live: 3600000  # 1 hour TTL
```

---

## 3. Common Issues & Fixes

### Kafka Issues

| Issue | Diagnosis | Fix |
|---|---|---|
| Consumer lag | `kafka-consumer-groups.sh --describe` | Add consumers, increase partitions |
| Message stuck | Check DLQ topic | Process or discard from DLQ |
| Broker down | Container health check | Restart, check disk space |

```bash
# Check consumer lag
docker exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group social-distro-workers-v3 \
  --describe
```

### Redis Issues

| Issue | Diagnosis | Fix |
|---|---|---|
| High memory | `INFO memory` | Enable eviction policy |
| Slow queries | `SLOWLOG GET 10` | Optimize data structures |
| Connection refused | Check container | Verify `SPRING_DATA_REDIS_HOST` |

```bash
# Redis CLI
docker exec -it social-distro-redis redis-cli
> INFO memory
> SLOWLOG GET 10
```

### PostgreSQL Issues

| Issue | Diagnosis | Fix |
|---|---|---|
| Slow queries | `pg_stat_statements` | Add indexes, EXPLAIN ANALYZE |
| Connection pool exhausted | Hikari metrics | Increase pool size |
| Lock contention | `pg_locks` | Review transaction boundaries |

```sql
-- Slow queries
SELECT query, calls, mean_exec_time 
FROM pg_stat_statements 
ORDER BY mean_exec_time DESC LIMIT 10;
```

---

## 4. Production Readiness Checklist

### ✅ What's Done

| Category | Status | Details |
|---|---|---|
| API Layer | ✅ | REST endpoints with validation |
| Persistence | ✅ | JPA + pgvector + Liquibase migrations |
| Messaging | ✅ | Kafka with fan-out pattern |
| Caching | ✅ | Redis for LLM responses |
| Resilience | ✅ | Circuit breaker, retries, DLQ |
| Observability | ✅ | Prometheus metrics endpoint |
| Concurrency | ✅ | Optimistic locking, idempotency |
| Schema | ✅ | Liquibase versioned migrations |

### ⚠️ What's Missing for Full Production

| Category | Status | What's Needed |
|---|---|---|
| Authentication | ❌ | Add Spring Security + JWT/OAuth2 |
| Rate Limiting | ❌ | Add Resilience4j RateLimiter |
| API Versioning | ❌ | `/api/v1/...` prefix |
| Health Checks | ⚠️ | Custom checks for Kafka, Redis |
| Secrets Management | ❌ | Vault or K8s Secrets |
| Horizontal Scaling | ⚠️ | Test with multiple instances |
| Load Testing | ⚠️ | Gatling/k6 stress test |
| Backup Strategy | ❌ | PostgreSQL pg_dump, Redis RDB |
| Logging | ⚠️ | Structured JSON logs, ELK stack |

---

## 5. Microservice Architecture Analysis

### Current State: Modular Monolith
This service is a **self-contained, deployable unit** that:
- Has its own database (PostgreSQL)
- Uses Kafka for internal async processing
- Exposes a REST API for external consumers

### Can It Run Standalone?
**Yes!** Other services can interact via:
```
POST /pods          → Create a pod
POST /pods/{id}/data → Add data
POST /indexing/pods/{id} → Trigger indexing
POST /pods/{id}/query → Query with RAG
```

### Should We Split Into Multiple Services?

| Potential Service | Pros | Cons |
|---|---|---|
| **Pod Service** (CRUD) | Simple, fast | Adds network hop |
| **Indexing Service** (Kafka workers) | Independent scaling | Shared DB complexity |
| **Query Service** (RAG) | Scale LLM calls separately | Needs vector access |

### Recommendation: **Keep as Modular Monolith**

**Why?**
1. **Single deployment** - easier ops, faster iteration
2. **Shared database** - no distributed transactions
3. **Team size** - one team can manage it
4. **Scaling** - Kafka consumers already scale independently

**When to split?**
- Different teams owning different parts
- Wildly different scaling needs (10x queries vs indexing)
- Separate SLAs (query must be 99.99%, indexing can be 99.9%)

### Architecture Evolution Path

```
Current: Modular Monolith
    ↓
Phase 2: Extract Query Service (if LLM becomes bottleneck)
    ↓
Phase 3: Extract Indexing Workers (if processing volume spikes)
    ↓
Phase 4: Full Microservices (if teams split ownership)
```

---

## 6. Quick Reference Commands

```bash
# View app logs
docker logs -f social-distro-backend

# Check Kafka topics
docker exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# View DLQ messages
docker exec kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic item-indexing-events-dlq \
  --from-beginning

# Redis cache stats
docker exec social-distro-redis redis-cli INFO stats

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus | grep indexing

# Health check
curl http://localhost:8080/actuator/health
```
