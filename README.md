# spring-ai-knowledge-base

A Spring Boot + Spring AI knowledge base service with document ingestion (Word/Excel/TXT via Apache Tika), chunking, embeddings, in-memory vector store, and RAG chat/search APIs.

Quick start
- Build: mvn clean package
- Run: mvn spring-boot:run
- OpenAPI UI: http://localhost:8080/swagger-ui.html

Configure a real LLM (optional)
- Set environment variables before running:
  - OPENAI_API_KEY (or spring.ai.openai.api-key)
  - spring.ai.openai.chat.options.model=gpt-4o-mini
  - spring.ai.openai.embedding.options.model=text-embedding-3-small
- Without keys, the chat falls back to a local extractive mode; search works locally.

Key endpoints
- POST /api/kb: create KB
- GET /api/kb: list KBs
- DELETE /api/kb/{id}: delete KB and its vectors
- POST /api/ingest/text: ingest free text into a KB
- POST /api/ingest/file: ingest a file (multipart) into a KB
- POST /api/search: semantic search in a KB
- POST /api/chat: RAG chat over a KB

Example
1) Create KB
curl -s -X POST http://localhost:8080/api/kb -H 'Content-Type: application/json' -d '{"name":"kb1","description":"demo"}'

2) Ingest text
curl -s -X POST http://localhost:8080/api/ingest/text -H 'Content-Type: application/json' -d '{"kbId":"<KB_ID>","text":"Hello Spring AI. This is a KB demo."}'

3) Search
curl -s -X POST http://localhost:8080/api/search -H 'Content-Type: application/json' -d '{"kbId":"<KB_ID>","query":"What is this?"}'

4) Chat
curl -s -X POST http://localhost:8080/api/chat -H 'Content-Type: application/json' -d '{"kbId":"<KB_ID>","question":"Summarize the KB", "topK":3}'

Notes
- Storage: in-memory for vectors and KB metadata; swap to a persistent store as needed.
- Parsing: Apache Tika standard parsers; add more as needed.
- Tests: unit tests cover chunking, embeddings, vector search, ingestion, and chat fallback.

