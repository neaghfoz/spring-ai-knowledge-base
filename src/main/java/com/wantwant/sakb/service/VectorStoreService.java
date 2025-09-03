package com.wantwant.sakb.service;

import com.wantwant.sakb.model.DocumentChunk;
import com.wantwant.sakb.util.Vectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Profile("!pg")
public class VectorStoreService implements VectorStore {
    private final Map<String, List<DocumentChunk>> byKb = new ConcurrentHashMap<>();

    @Override
    public void addAll(String kbId, List<DocumentChunk> chunks) {
        byKb.computeIfAbsent(kbId, k -> Collections.synchronizedList(new ArrayList<>())).addAll(chunks);
    }

    @Override
    public List<DocumentChunk> getAll(String kbId) {
        return new ArrayList<>(byKb.getOrDefault(kbId, List.of()));
    }

    @Override
    public void deleteKb(String kbId) {
        byKb.remove(kbId);
    }

    @Override
    public List<Scored<DocumentChunk>> search(String kbId, float[] queryEmbedding, int topK) {
        List<DocumentChunk> chunks = byKb.getOrDefault(kbId, List.of());
        return chunks.stream()
                .map(c -> new Scored<>(c, Vectors.cosine(queryEmbedding, c.getEmbedding())))
                .sorted((a,b) -> Double.compare(b.score(), a.score()))
                .limit(topK)
                .collect(Collectors.toList());
    }
}
