-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto; -- for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS vector;   -- pgvector

-- Knowledge bases table
CREATE TABLE IF NOT EXISTS knowledge_bases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Knowledge base chunks table
CREATE TABLE IF NOT EXISTS kb_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kb_id TEXT NOT NULL,
    source_name TEXT NOT NULL,
    chunk_index INT NOT NULL,
    text TEXT NOT NULL,
    embedding VECTOR(1536) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_kb_chunks_kb_id ON kb_chunks (kb_id);
-- IVF Flat index for cosine similarity
CREATE INDEX IF NOT EXISTS idx_kb_chunks_embedding_cos ON kb_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

