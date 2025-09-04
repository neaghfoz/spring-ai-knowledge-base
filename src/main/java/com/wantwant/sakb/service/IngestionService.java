package com.wantwant.sakb.service;

import com.wantwant.sakb.model.DocumentChunk;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Service
public class IngestionService {
    private final KnowledgeBaseService kbService;
    private final DocumentParserService parserService;
    private final TextChunkerService chunkerService;
    private final Embeddings embeddingService;
    private final VectorStore vectorStoreService;

    public IngestionService(KnowledgeBaseService kbService,
                            DocumentParserService parserService,
                            TextChunkerService chunkerService,
                            Embeddings embeddingService,
                            VectorStore vectorStoreService) {
        this.kbService = kbService;
        this.parserService = parserService;
        this.chunkerService = chunkerService;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    public int ingestText(String kbId, String text, String sourceName, Map<String, Object> extraMeta) throws Exception {
        ensureKb(kbId);
        if (sourceName == null || sourceName.isBlank()) sourceName = "text:" + Math.abs(Objects.requireNonNullElse(text, "").hashCode());
        List<String> chunks = chunkerService.chunk(text);
        List<DocumentChunk> docs = new ArrayList<>();
        int idx = 0;
        for (String c : chunks) {
            float[] emb = embeddingService.embed(c);
            Map<String, Object> meta = new HashMap<>();
            if (extraMeta != null) meta.putAll(extraMeta);
            docs.add(new DocumentChunk(kbId, sourceName, idx++, c, emb, meta));
        }
        vectorStoreService.addAll(kbId, docs);
        return docs.size();
    }

    public int ingestFile(String kbId, MultipartFile file) throws Exception {
        ensureKb(kbId);
        try (InputStream is = file.getInputStream()) {
            var parsed = parserService.parse(is, file.getOriginalFilename(), file.getContentType());
            String source = file.getOriginalFilename();
            if (source == null || source.isBlank()) source = "upload:" + file.getName();
            Map<String, Object> meta = new HashMap<>(parsed.metadata);
            meta.put("ext", FilenameUtils.getExtension(source));
            return ingestText(kbId, parsed.text, source, meta);
        }
    }

    // ===== Management APIs =====
    public List<String> listSources(String kbId) {
        ensureKb(kbId);
        return vectorStoreService.listSources(kbId);
    }

    public List<DocumentChunk> listChunks(String kbId, String sourceName) {
        ensureKb(kbId);
        return vectorStoreService.list(kbId, sourceName);
    }

    public int countChunks(String kbId, String sourceName) {
        ensureKb(kbId);
        return vectorStoreService.count(kbId, sourceName);
    }

    public void deleteSource(String kbId, String sourceName) {
        ensureKb(kbId);
        vectorStoreService.deleteBySource(kbId, sourceName);
    }

    public void deleteChunk(String kbId, String sourceName, int chunkIndex) {
        ensureKb(kbId);
        vectorStoreService.deleteChunk(kbId, sourceName, chunkIndex);
    }

    public void upsertChunk(String kbId, String sourceName, int chunkIndex, String text, Map<String, Object> metadata) {
        ensureKb(kbId);
        float[] emb = embeddingService.embed(text == null ? "" : text);
        Map<String, Object> meta = new HashMap<>();
        if (metadata != null) meta.putAll(metadata);
        DocumentChunk chunk = new DocumentChunk(kbId, sourceName, chunkIndex, text, emb, meta);
        vectorStoreService.upsert(kbId, chunk);
    }

    private void ensureKb(String kbId) {
        kbService.get(kbId).orElseThrow(() -> new com.wantwant.sakb.exception.NotFoundException("KnowledgeBase not found: " + kbId));
    }
}
