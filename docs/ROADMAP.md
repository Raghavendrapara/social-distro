# App Summary & Roadmap

## Product Vision
![DataHub Ecosystem Vision](file:///C:/Users/rsrsr/.gemini/antigravity/brain/4b35da74-f8ae-4da8-ac2f-eb7aac9119f2/datahub_ecosystem_vision_1768330716088.png)

## Application Flow

```mermaid
flowchart TB
    subgraph "User Flow"
        U1[Create Pod] --> U2[Add Data Items]
        U2 --> U3[Trigger Indexing]
        U3 --> U4[Query with RAG]
    end

    subgraph "Indexing Pipeline"
        I1[IndexingService] -->|Kafka| I2[IndexingWorker]
        I2 -->|Fan-out| I3[ItemIndexingWorker]
        I3 -->|Embedding| I4[Ollama]
        I4 --> I5[(pgvector)]
    end

    subgraph "Query Pipeline"
        Q1[Question] --> Q2[Generate Embedding]
        Q2 --> Q3[Vector Similarity Search]
        Q3 --> Q4[LLM + Context]
        Q4 --> Q5[Answer]
    end

    U3 --> I1
    U4 --> Q1
```

## Data Flow

```mermaid
sequenceDiagram
    participant User
    participant API
    participant Kafka
    participant Worker
    participant Ollama
    participant PostgreSQL
    participant Redis

    Note over User,Redis: Indexing Flow
    User->>API: POST /pods/{id}/data
    API->>PostgreSQL: Save DataItem
    User->>API: POST /indexing/pods/{id}
    API->>Kafka: PodIndexingEvent
    Kafka->>Worker: Consume
    Worker->>Kafka: Fan-out ItemEvents
    Worker->>Ollama: Generate Embedding
    Ollama-->>Worker: Vector
    Worker->>PostgreSQL: Store in pgvector

    Note over User,Redis: Query Flow
    User->>API: POST /pods/{id}/query
    API->>Redis: Check Cache
    alt Cache Hit
        Redis-->>API: Cached Answer
    else Cache Miss
        API->>Ollama: Embed Question
        API->>PostgreSQL: Vector Search
        API->>Ollama: Generate Answer
        API->>Redis: Cache Answer
    end
    API-->>User: Answer
```

---

## Market Success Roadmap

### Phase 1: Core Product (✅ Done)
- [x] Pod-based data organization
- [x] Async indexing with Kafka
- [x] RAG query with vector search
- [x] Resilience (circuit breaker, retries)
- [x] Observability (Prometheus, Grafana, Zipkin)

### Phase 2: User Experience
| Feature | Impact |
|---|---|
| **Web UI** | Dashboard for managing pods, uploading files, querying |
| **File Upload** | PDF, DOCX, TXT ingestion with chunking |
| **Chat Interface** | Conversational RAG with history |
| **Real-time Progress** | WebSocket for indexing status |

### Phase 3: Enterprise Features
| Feature | Value Proposition |
|---|---|
| **Multi-tenancy** | Isolated data per organization |
| **RBAC** | Role-based access control |
| **SSO** | SAML/OIDC integration |
| **Audit Logs** | Compliance tracking |
| **Data Encryption** | At-rest and in-transit |

### Phase 4: AI Enhancements
| Feature | Technology |
|---|---|
| **Multi-model Support** | OpenAI, Anthropic, Gemini, local Ollama |
| **Hybrid Search** | Vector + keyword (BM25) |
| **Agentic RAG** | Tool use, multi-step reasoning |
| **Citations** | Source attribution in answers |
| **Summarization** | Auto-generate pod summaries |

### Phase 5: Scale & Distribution
| Feature | Benefit |
|---|---|
| **Multi-region** | Low latency globally |
| **Edge Deployment** | On-prem for sensitive data |
| **Usage Analytics** | Query insights, popular topics |
| **API Rate Limiting** | Fair usage, monetization |

---

## Competitive Differentiators

| Differentiator | Description |
|---|---|
| **Open Source Core** | Self-hostable, no vendor lock-in |
| **Model Agnostic** | Use any LLM (local or cloud) |
| **Developer-First** | Clean API, comprehensive docs |
| **Event-Driven** | Scales to millions of documents |
| **Observable by Default** | Built-in Prometheus metrics |

---

## Tech Stack Summary

```
┌─────────────────────────────────────────────────────────┐
│                    API Layer (REST)                     │
├─────────────────────────────────────────────────────────┤
│  Spring Boot 4 │ Virtual Threads │ Hexagonal Arch       │
├─────────────────────────────────────────────────────────┤
│       Kafka (Events)    │    Redis (Cache)              │
├─────────────────────────────────────────────────────────┤
│  PostgreSQL + pgvector  │     Ollama (LLM)              │
├─────────────────────────────────────────────────────────┤
│  Prometheus │ Grafana │ Zipkin │ Liquibase              │
└─────────────────────────────────────────────────────────┘
```
