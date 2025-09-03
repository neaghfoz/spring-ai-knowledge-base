# spring-ai-knowledge-base

A Spring Boot + Spring AI knowledge base service with document ingestion (Word/Excel/TXT via Apache Tika), chunking, embeddings, persistent vector store (Postgres + pgvector), and RAG chat/search APIs.

Quick start (local, default)
- Start dependencies (Postgres + Ollama):
  - docker compose up -d
  - Optional: pre-pull models so first call is fast:
    - docker exec -it sakb-ollama ollama pull qwen3:8b
    - docker exec -it sakb-ollama ollama pull nomic-embed-text
- Run the app:
  - mvn spring-boot:run
- OpenAPI UI:
  - http://localhost:8080/swagger-ui/index.html

What’s enabled by default
- Database: Postgres (ankane/pgvector) on localhost:5432 (user/pass/db: sakb/sakb/sakb)
  - Schema and extensions (pgvector) auto-initialized on startup.
  - Vector column size: 1536 (configurable via kb.vector.dim).
- Local AI provider: Ollama on localhost:11434
  - Chat model: qwen3:8b
  - Embedding model: nomic-embed-text
- OpenAI is disabled and its auto-config is excluded to avoid requiring API keys.

Switch providers (optional)
- OpenAI: create application-local.properties and run with -Dspring-boot.run.profiles=local, or set env vars. Example:
  - spring.ai.openai.enabled=true
  - openai.api-key=${OPENAI_API_KEY}
  - spring.ai.openai.chat.options.model=gpt-4o-mini
  - spring.ai.openai.embedding.options.model=text-embedding-3-small
  - spring.autoconfigure.exclude=

Key endpoints
- POST /api/kb: create KB
- GET /api/kb: list KBs
- DELETE /api/kb/{id}: delete KB and its vectors
- POST /api/ingest/text: ingest free text into a KB
- POST /api/ingest/file: ingest a file (multipart) into a KB
- POST /api/search: semantic search in a KB
- POST /api/chat: RAG chat over a KB

Notes
- Vector store:
  - By default, uses Postgres (profile "pg" is auto-activated). An in-memory implementation is used if you run without the pg profile.
  - If your embedding model dimension differs, the service pads/truncates vectors to kb.vector.dim (1536 by default) so the DB column size matches.
- Fallbacks:
  - If no ChatModel is available or the local model is down, chat gracefully falls back to an extractive answer from context.
- Tests: unit tests cover chunking, embeddings, vector search, ingestion, and chat fallback.
