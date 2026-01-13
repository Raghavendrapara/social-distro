# Scalability, Maintainability & Modernization Plan (2026 Edition)

## Analysis Summary

### Critical Bottlenecks (Scalability)
1.  **Synchronous "Job" Processing**: One large pod blocks the entire worker queue.
    -   *Fix*: **Fan-out Architecture** (Split Job -> Items).
2.  **Single Point of Failure**: Local Ollama instance.
    -   *Fix*: Load Balancer + Circuit Breakers.

### Code Quality & Ecosystem (Modern Standards)
1.  **Schema Management**: Currently uses basic `schema.sql`.
    -   *Fix*: Migrate to **Liquibase**.
2.  **Vector Store Coupling**: Tightly coupled to `pgvector`.
    -   *Fix*: **Hexagonal Architecture** (VectorStore Port).
3.  **Observability**: Missing metrics.
    -   *Fix*: Micrometer + Prometheus + Tracing (Zipkin).

### Future Extensibility (AI & Social)
1.  **User Clustering**: Need `VectorStore` to support pod-level embedding retrieval.

## Proposed Changes

### Phase 1: Modern Foundations (The "Hygiene" Phase)

#### [MODIFY] [pom.xml](file:///c:/Users/rsrsr/IdeaProjects/social-distro/pom.xml)
- Add `liquibase-core`.
- Add `micrometer-registry-prometheus`, `micrometer-tracing-bridge-brave`.
- Add `spring-boot-starter-data-redis`.
- **Note**: `resilience4j-spring-boot3` is already present. We will configure it rather than add it.

#### [NEW] [docker-compose.yml](file:///c:/Users/rsrsr/IdeaProjects/social-distro/docker-compose.yml)
- Add **Redis** service (Alpine).

#### [NEW] [db/changelog/db.changelog-master.yaml](file:///c:/Users/rsrsr/IdeaProjects/social-distro/src/main/resources/db/changelog/db.changelog-master.yaml)
- Initial schema definition.

### Phase 2: Resilience & Caching (The "Robustness" Phase)

#### [NEW] [CacheConfig.java](file:///c:/Users/rsrsr/IdeaProjects/social-distro/src/main/java/com/raghav/datahub/config/CacheConfig.java)
- Configure Redis Cache Manager.
- **Why Redis?**: For a distributed system (multiple instances), a local cache (like Caffeine/EHCache) leads to data inconsistency. Redis provides a shared, consistent cache state across all application nodes.

#### [MODIFY] [application.yaml](file:///c:/Users/rsrsr/IdeaProjects/social-distro/src/main/resources/application.yaml)
- Configure `resilience4j.circuitbreaker` instances (e.g., `llm`, `vectorStore`).

#### [MODIFY] [OllamaLlmClient.java](file:///c:/Users/rsrsr/IdeaProjects/social-distro/src/main/java/com/raghav/datahub/service/llm/OllamaLlmClient.java)
- Apply `@CircuitBreaker(name = "llm")`.
- Apply `@Cacheable` for identical prompts.

### Phase 3: Scalability & Vector Abstraction

#### [NEW] [VectorStore.java](file:///c:/Users/rsrsr/IdeaProjects/social-distro/src/main/java/com/raghav/datahub/domain/port/VectorStore.java)
- Interface: `void saveChunk(VectorChunk chunk)`, `List<float[]> getPodEmbeddings(String podId)`.

#### [MODIFY] [IndexingWorker.java](file:///c:/Users/rsrsr/IdeaProjects/social-distro/src/main/java/com/raghav/datahub/service/indexing/IndexingWorker.java)
- Logic change: Iterate items -> Send `ItemIndexingEvent` to Kafka.

#### [NEW] [ItemIndexingWorker.java](file:///c:/Users/rsrsr/IdeaProjects/social-distro/src/main/java/com/raghav/datahub/service/indexing/ItemIndexingWorker.java)
- Consumes `ItemIndexingEvent`.
- **Metrics**: `Timer.sample()` around embedding generation.
- Calls `VectorStore.saveChunk()`.

## Verification Plan

### Automated Tests
1.  **Stress Test**: Run `stress_test.py` with 1000 items. 
2.  **Chaos Test**: Stop Ollama container, verify Circuit Breaker opens.
3.  **Observability**: Check Prometheus endpoint.

### Manual Verification
- Verify Redis keys created.
