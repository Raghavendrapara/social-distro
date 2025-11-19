# ChronicleAI – Personal Knowledge Intelligence Engine

ChronicleAI is a lightweight backend that organizes user data into “pods”, indexes it in parallel, and enables intelligent querying using an LLM-capable pipeline. Built with Java 21 and Spring Boot using clean layered architecture, DTO-first design, MapStruct mapping, and a high-performance CompletableFuture-based indexing engine.

## Features
- Pod-based data storage
- Parallel indexing (chunking, retries, timeouts)
- Mock LLM query API (easily replaceable with OpenAI / Groq / Ollama)
- Clean controller/service/domain architecture
- MapStruct + validation
- Metrics endpoint: `/metrics/indexing`

## Run
mvn clean compile
mvn spring-boot:run

## API
POST /pods — create pod
POST /pods/{id}/data — add data
POST /pods/{id}/index — start indexing
POST /pods/{id}/query — ask questions
GET  /metrics/indexing — view metrics

## Roadmap
Add real LLM, embeddings, vector DB, full RAG pipeline, and Docker deployment.
