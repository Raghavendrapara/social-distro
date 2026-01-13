# Dependency Analysis: Social Distro

A comprehensive guide to every dependency used in this project, why it was chosen, its relevance in 2026, and available alternatives.

---

## Core Framework

### Spring Boot 4.0.1
| Aspect | Details |
|---|---|
| **Purpose** | Application framework providing auto-configuration, embedded server, DI |
| **Why in 2026** | Industry standard for Java backends. v4.0 brings native GraalVM support, virtual threads (Project Loom), and improved observability |
| **Alternatives** | **Quarkus** (faster startup, Kubernetes-native), **Micronaut** (compile-time DI), **Helidon** (Oracle's lightweight framework) |
| **Tradeoff** | Larger memory footprint than Quarkus, but richer ecosystem and documentation |

---

## Web Layer

### spring-boot-starter-web
| Aspect | Details |
|---|---|
| **Purpose** | REST API support with embedded Tomcat/Jetty/Undertow |
| **Why in 2026** | Virtual threads in Spring Boot 4 eliminate the need for reactive programming for most I/O-bound workloads |
| **Alternatives** | **Spring WebFlux** (reactive), **Vert.x** (event loop), **Javalin** (lightweight) |
| **Tradeoff** | Blocking model simpler to debug; reactive has higher throughput but steep learning curve |

### spring-boot-starter-validation
| Aspect | Details |
|---|---|
| **Purpose** | Bean Validation (JSR-380) for request DTOs |
| **Why in 2026** | Still the standard for declarative validation (`@NotBlank`, `@Size`) |
| **Alternatives** | Manual validation, **YAVI** (fluent functional validation) |
| **Tradeoff** | Annotation-based is cleaner for simple cases; YAVI better for complex conditional logic |

---

## Persistence

### spring-boot-starter-data-jpa
| Aspect | Details |
|---|---|
| **Purpose** | JPA/Hibernate ORM with Spring Data repositories |
| **Why in 2026** | Mature, widely understood. Spring Data 4 improves virtual thread support |
| **Alternatives** | **jOOQ** (type-safe SQL), **MyBatis** (SQL mapping), **JDBC Template** (raw SQL) |
| **Tradeoff** | JPA abstracts SQL (good for CRUD, bad for complex queries); jOOQ for SQL-first |

### PostgreSQL Driver
| Aspect | Details |
|---|---|
| **Purpose** | JDBC driver for PostgreSQL |
| **Why in 2026** | PostgreSQL is the "default database" for new projects - JSON, vector, full-text search |
| **Alternatives** | **MySQL**, **CockroachDB** (distributed), **YugabyteDB** (distributed PostgreSQL-compatible) |
| **Tradeoff** | Single-node PostgreSQL scales well vertically; use CockroachDB for global distribution |

### hibernate-vector
| Aspect | Details |
|---|---|
| **Purpose** | Hibernate support for pgvector `vector` type |
| **Why in 2026** | Native AI/ML integration in relational DBs is the 2026 trend |
| **Alternatives** | **Native queries**, dedicated vector DBs (**Pinecone**, **Milvus**, **Qdrant**) |
| **Tradeoff** | pgvector keeps data in one DB; Pinecone scales better but adds operational complexity |

### spring-boot-starter-liquibase
| Aspect | Details |
|---|---|
| **Purpose** | Database schema version control and migrations |
| **Why in 2026** | Essential for CI/CD and reproducible environments |
| **Alternatives** | **Flyway** (simpler, SQL-only), **Atlas** (HCL-based, declarative), **manual scripts** |
| **Tradeoff** | Liquibase supports XML/YAML/SQL; Flyway is simpler but less flexible |

---

## Messaging

### spring-kafka
| Aspect | Details |
|---|---|
| **Purpose** | Apache Kafka integration for event-driven architecture |
| **Why in 2026** | De-facto standard for high-throughput event streaming |
| **Alternatives** | **RabbitMQ** (queueing), **AWS SQS/SNS** (managed), **Apache Pulsar** (unified queue+stream), **NATS** (lightweight) |
| **Tradeoff** | Kafka has operational overhead; SQS is simpler but vendor-locked; Pulsar is powerful but newer ecosystem |

---

## Caching

### spring-boot-starter-data-redis
| Aspect | Details |
|---|---|
| **Purpose** | Distributed caching with Redis |
| **Why in 2026** | Redis 8 adds enhanced data types, better clustering. Still the go-to for sub-ms caching |
| **Alternatives** | **Memcached** (simpler), **Caffeine** (in-process), **Hazelcast** (embedded distributed), **Valkey** (Redis fork) |
| **Tradeoff** | Redis adds network hop; Caffeine is faster but not shared across instances |

---

## Resilience

### resilience4j-spring-boot3
| Aspect | Details |
|---|---|
| **Purpose** | Circuit breaker, retry, rate limiter, bulkhead patterns |
| **Why in 2026** | Lightweight, modular, works with virtual threads (unlike Hystrix) |
| **Alternatives** | **Sentinel** (Alibaba, richer features), **Failsafe** (simpler API), **Spring Retry** (basic retries only) |
| **Tradeoff** | Resilience4j is annotation-driven; Sentinel has a dashboard but heavier |

### spring-boot-starter-aspectj
| Aspect | Details |
|---|---|
| **Purpose** | AOP support for `@CircuitBreaker`, `@Cacheable` proxies |
| **Why in 2026** | Required for annotation-based cross-cutting concerns |
| **Alternatives** | **Compile-time AOP** (AspectJ weaving), **manual proxies** |
| **Tradeoff** | Runtime AOP is simpler; compile-time AOP is faster but complex build setup |

---

## Observability

### micrometer-registry-prometheus
| Aspect | Details |
|---|---|
| **Purpose** | Exports metrics in Prometheus format (`/actuator/prometheus`) |
| **Why in 2026** | Prometheus + Grafana is the standard observability stack |
| **Alternatives** | **Datadog**, **New Relic**, **OpenTelemetry Collector** (vendor-agnostic) |
| **Tradeoff** | Prometheus is OSS and free; Datadog is managed but expensive |

### micrometer-tracing-bridge-brave + zipkin-reporter-brave
| Aspect | Details |
|---|---|
| **Purpose** | Distributed tracing with Zipkin |
| **Why in 2026** | Essential for debugging microservices; Brave is mature |
| **Alternatives** | **Jaeger**, **OpenTelemetry** (converging standard), **AWS X-Ray** |
| **Tradeoff** | OpenTelemetry is the future but Brave is battle-tested |

### spring-boot-starter-actuator
| Aspect | Details |
|---|---|
| **Purpose** | Production-ready features: health checks, metrics, info endpoints |
| **Why in 2026** | Essential for Kubernetes liveness/readiness probes |
| **Alternatives** | Custom endpoints, **SmallRye Health** (Quarkus) |
| **Tradeoff** | Actuator is comprehensive; may expose sensitive info if not secured |

---

## Serialization

### jackson-databind
| Aspect | Details |
|---|---|
| **Purpose** | JSON serialization/deserialization |
| **Why in 2026** | Industry standard, excellent Spring integration |
| **Alternatives** | **Gson** (simpler), **Moshi** (Kotlin-friendly), **fastjson2** (faster, Chinese origin) |
| **Tradeoff** | Jackson is feature-rich but verbose config; Gson is simpler for basic use |

### jackson-datatype-jsr310
| Aspect | Details |
|---|---|
| **Purpose** | Java 8+ date/time (`Instant`, `LocalDate`) support for Jackson |
| **Why in 2026** | Required for proper `java.time` serialization |
| **Alternatives** | None - this is the standard module |
| **Tradeoff** | Must register `JavaTimeModule` or use Spring Boot auto-config |

---

## Code Generation

### Lombok (1.18.40)
| Aspect | Details |
|---|---|
| **Purpose** | Reduces boilerplate (`@Getter`, `@RequiredArgsConstructor`, `@Slf4j`) |
| **Why in 2026** | Still widely used, though Java records reduce some need |
| **Alternatives** | **Java Records** (for DTOs), **Kotlin data classes**, **Immutables** (annotation-based) |
| **Tradeoff** | Lombok is magic (compile-time); records are explicit but limited to immutable DTOs |

### MapStruct (1.6.3)
| Aspect | Details |
|---|---|
| **Purpose** | Compile-time bean mapping (Entity ↔ Domain ↔ DTO) |
| **Why in 2026** | Type-safe, no reflection, excellent performance |
| **Alternatives** | **ModelMapper** (runtime), **Dozer** (legacy), manual mapping |
| **Tradeoff** | MapStruct requires annotation processing; ModelMapper is simpler but slower |

### lombok-mapstruct-binding
| Aspect | Details |
|---|---|
| **Purpose** | Fixes annotation processor ordering between Lombok and MapStruct |
| **Why in 2026** | Required when using both Lombok and MapStruct |
| **Alternatives** | Order processors manually in Maven |
| **Tradeoff** | Small binding library, essential for compatibility |

---

## Build & Runtime

### Java 25
| Aspect | Details |
|---|---|
| **Purpose** | Latest LTS with virtual threads, pattern matching, records |
| **Why in 2026** | Virtual threads (Project Loom) are game-changing for I/O-bound apps |
| **Alternatives** | **Java 21** (previous LTS), **Kotlin**, **GraalVM Native Image** |
| **Tradeoff** | Bleeding edge; some libraries may lag in support |

### Maven
| Aspect | Details |
|---|---|
| **Purpose** | Build tool and dependency management |
| **Why in 2026** | Still dominant for enterprise Java |
| **Alternatives** | **Gradle** (faster, flexible), **Bazel** (monorepo), **Mill** (Scala-inspired) |
| **Tradeoff** | Maven is verbose but predictable; Gradle is powerful but complex |

---

## Summary: 2026 Tech Radar

| Category | Used | Trending Alternative |
|---|---|---|
| Framework | Spring Boot 4 | Quarkus (for cloud-native) |
| Messaging | Kafka | Pulsar, NATS |
| Cache | Redis | Valkey (Redis fork) |
| Vector DB | pgvector | Pinecone, Qdrant |
| Observability | Prometheus | OpenTelemetry |
| Resilience | Resilience4j | Sentinel |
| Mapping | MapStruct | Records + manual |
| Schema | Liquibase | Atlas |

---

## Version Matrix

| Dependency | Version | Latest Stable (Jan 2026) |
|---|---|---|
| Spring Boot | 4.0.1 | 4.0.1 |
| Java | 25 | 25 |
| MapStruct | 1.6.3 | 1.6.3 |
| Lombok | 1.18.40 | 1.18.40 |
| Resilience4j | 2.3.0 | 2.3.0 |
