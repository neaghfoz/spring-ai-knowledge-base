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

    @Override
    public List<DocumentChunk> list(String kbId, String sourceName) {
        return byKb.getOrDefault(kbId, List.of()).stream()
                .filter(c -> Objects.equals(sourceName, c.getSourceName()))
                .sorted(Comparator.comparingInt(DocumentChunk::getChunkIndex))
                .collect(Collectors.toList());
    }

    @Override
    public int count(String kbId, String sourceName) {
        return (int) byKb.getOrDefault(kbId, List.of()).stream()
                .filter(c -> Objects.equals(sourceName, c.getSourceName()))
                .count();
    }

    @Override
    public void deleteBySource(String kbId, String sourceName) {
        List<DocumentChunk> list = byKb.getOrDefault(kbId, List.of());
        list.removeIf(c -> Objects.equals(sourceName, c.getSourceName()));
    }

    @Override
    public void deleteChunk(String kbId, String sourceName, int chunkIndex) {
        List<DocumentChunk> list = byKb.getOrDefault(kbId, List.of());
        list.removeIf(c -> Objects.equals(sourceName, c.getSourceName()) && c.getChunkIndex() == chunkIndex);
    }

    @Override
    public void upsert(String kbId, DocumentChunk chunk) {
        deleteChunk(kbId, chunk.getSourceName(), chunk.getChunkIndex());
        addAll(kbId, List.of(chunk));
    }

    // Helper for listing sources
    public List<String> listSources(String kbId) {
        return byKb.getOrDefault(kbId, List.of()).stream()
                .map(DocumentChunk::getSourceName)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }
}
