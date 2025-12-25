# Personal Knowledge Intelligence Engine

A lightweight backend that organizes user data into “pods”, indexes it in parallel, and enables intelligent querying using an LLM-capable pipeline.

Built with Java 21 and Spring Boot using clean layered architecture, DTO-first design, MapStruct mapping, and a high-performance CompletableFuture-based indexing engine.

## Features
- Pod-based data storage
- Parallel indexing (chunking, retries, timeouts)
- Mock LLM query API (easily replaceable with OpenAI / Grok / Ollama)
- Clean controller/service/domain architecture
- MapStruct + validation


## Run
docker-compose up -d --build

## API
POST /pods — create pod

POST /pods/{id}/data — add data

POST /pods/{id}/index — start indexing

POST /pods/{id}/query — ask questions


## Roadmap
Host on Cloud, reduce latency
