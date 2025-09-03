package com.wantwant.sakb.service;

import com.wantwant.sakb.model.DocumentChunk;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VectorStoreServiceTest {

    @Test
    void search_ranksSimilarHigher() {
        EmbeddingService embedding = new EmbeddingService();
        VectorStoreService store = new VectorStoreService();
        String kb = "kb1";

        float[] eHello1 = embedding.embed("hello world");
        float[] eHello2 = embedding.embed("hello there");
        float[] eOther = embedding.embed("goodbye moon");

        store.addAll(kb, List.of(
                new DocumentChunk(kb, "hello1", 0, "hello world", eHello1, Map.of()),
                new DocumentChunk(kb, "hello2", 0, "hello there", eHello2, Map.of()),
                new DocumentChunk(kb, "other", 0, "goodbye moon", eOther, Map.of())
        ));

        float[] q = embedding.embed("hello");
        var hits = store.search(kb, q, 2);
        assertEquals(2, hits.size());
        assertTrue(hits.get(0).score() >= hits.get(1).score());
        String topSource = hits.get(0).item().getSourceName();
        assertTrue(topSource.startsWith("hello"));
    }
}

