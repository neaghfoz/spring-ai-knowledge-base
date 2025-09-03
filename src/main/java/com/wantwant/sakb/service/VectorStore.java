package com.wantwant.sakb.service;

import com.wantwant.sakb.model.DocumentChunk;

import java.util.List;

public interface VectorStore {
    void addAll(String kbId, List<DocumentChunk> chunks);
    List<DocumentChunk> getAll(String kbId);
    void deleteKb(String kbId);
    List<Scored<DocumentChunk>> search(String kbId, float[] queryEmbedding, int topK);
}
