package com.wantwant.sakb.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class DocumentChunk {
    private final String id = UUID.randomUUID().toString();
    private final String kbId;
    private final String sourceName;
    private final int chunkIndex;
    private final String text;
    private final float[] embedding;
    private final Instant createdAt = Instant.now();
    private final Map<String, Object> metadata;

    public DocumentChunk(String kbId, String sourceName, int chunkIndex, String text, float[] embedding, Map<String, Object> metadata) {
        this.kbId = kbId;
        this.sourceName = sourceName;
        this.chunkIndex = chunkIndex;
        this.text = text;
        this.embedding = embedding;
        this.metadata = metadata;
    }

    public String getId() { return id; }
    public String getKbId() { return kbId; }
    public String getSourceName() { return sourceName; }
    public int getChunkIndex() { return chunkIndex; }
    public String getText() { return text; }
    public float[] getEmbedding() { return embedding; }
    public Instant getCreatedAt() { return createdAt; }
    public Map<String, Object> getMetadata() { return metadata; }
}

