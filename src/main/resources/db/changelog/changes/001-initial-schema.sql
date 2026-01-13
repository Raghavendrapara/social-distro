-- liquibase formatted sql

-- changeset raghav-ai:1-initial-schema
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE pods (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    owner_user_id VARCHAR(100) NOT NULL
);

CREATE INDEX idx_pods_owner ON pods(owner_user_id);

CREATE TABLE data_items (
    id VARCHAR(255) PRIMARY KEY,
    pod_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_data_items_pods FOREIGN KEY (pod_id) REFERENCES pods(id)
);

CREATE INDEX idx_data_items_pod_id ON data_items(pod_id);

CREATE TABLE vector_chunks (
    id VARCHAR(255) PRIMARY KEY,
    pod_id VARCHAR(255) NOT NULL,
    content TEXT,
    model_version VARCHAR(50) NOT NULL,
    embedding vector(768) -- 768 for nomic-embed-text (v1.5) / phi3 hidden state
);

CREATE INDEX idx_vector_chunks_pod_id ON vector_chunks(pod_id);

CREATE INDEX idx_vector_chunks_embedding ON vector_chunks USING hnsw (embedding vector_l2_ops) WITH (m = 16, ef_construction = 64);

CREATE TABLE indexing_jobs (
    job_id VARCHAR(255) PRIMARY KEY,
    pod_id VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    error_message VARCHAR(2000)
);

CREATE TABLE pod_indexes (
    pod_id VARCHAR(255) PRIMARY KEY,
    combined_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
