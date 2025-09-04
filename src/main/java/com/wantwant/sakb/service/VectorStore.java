package com.wantwant.sakb.service;

import com.wantwant.sakb.model.DocumentChunk;

import java.util.List;

public interface VectorStore {
    void addAll(String kbId, List<DocumentChunk> chunks);
    List<DocumentChunk> getAll(String kbId);
    void deleteKb(String kbId);
    List<Scored<DocumentChunk>> search(String kbId, float[] queryEmbedding, int topK);

    // Management operations
    List<DocumentChunk> list(String kbId, String sourceName);
    int count(String kbId, String sourceName);
    void deleteBySource(String kbId, String sourceName);
    void deleteChunk(String kbId, String sourceName, int chunkIndex);
    void upsert(String kbId, DocumentChunk chunk);
    List<String> listSources(String kbId);
}
