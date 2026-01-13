# Social Distro - Personal Data Insight Hub

A distributed, scalable backend system for personal data management and AI-powered insights, built with modern Java and hexagonal architecture.

## Functionality
- **Pod Management**: Create and manage isolated data pods ("Knowledge Bases") for users.
- **Data Ingestion**: Add text data to pods.
- **Scalable Indexing**:
    - **Fan-out Architecture**: Single job splits into granular item events via Kafka.
    - **Asynchronous Processing**: `ItemIndexingWorker` processes embeddings in parallel.
    - **Vector Storage**: Uses `pgvector` with HNSW indexes for high-performance similarity search.
- **AI/LLM Integration**:
    - Uses **Ollama** (local inference) for Embeddings (`nomic-embed-text`) and Chat (`phi3:mini`).
    - **RAG (Retrieval-Augmented Generation)**: Contextual answers based on pod data.
- **Resilience**:
    - **Circuit Breakers**: Fallback mechanisms when LLM is overloaded/down.
    - **Retries**: Automatic retries for indexing failures.
- **Performance**:
    - **Redis Caching**: Caches embeddings and LLM responses.
    - **Observability**: Prometheus metrics for monitoring throughput and latencies.

## Data Flow
1. **Ingest**: POST `/pods/{id}/data` -> Saves to DB.
2. **Index**: POST `/indexing/pods/{id}` -> Creates Job -> Pushes `PodIndexingEvent` to Kafka.
3. **Fan-out**: `IndexingWorker` consumes event -> Streams items -> Pushes `ItemIndexingEvent`s to Kafka.
4. **Process**: `ItemIndexingWorker` consumes item -> Generates Embedding (Ollama) -> Saves to `vector_chunks` (Postgres).
5. **Query**: POST `/pods/{id}/query` -> Generates Embedding -> Searches Vector Store -> augmented prompt to LLM -> Returns Answer.

## Deployment
- **Docker Compose**: Orchestrates App, Postgres, Redis, Kafka, Zookeeper, and Ollama.
- **Liquibase**: Manages database schema changes and migrations.

## Future Roadmap
- **Production Resilience**: Chaos testing suites and advanced recovery scenarios.
- **User Clustering**: Cross-pod analytics and social graphs.
- **Model Evolution**: Support for versioning embeddings when upgrading LLM models.
- **Cloud Native**: Migration to Kubernetes (K8s) deployment.
