package com.wantwant.sakb.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IngestionServiceTest {

    @Test
    void ingestText_addsChunksToVectorStore() throws Exception {
    KnowledgeBaseService kb = new InMemoryKnowledgeBaseService();
        var kbObj = kb.create("kb", "test");
        DocumentParserService parser = new DocumentParserService();
        TextChunkerService chunker = new TextChunkerService();
        EmbeddingService embedding = new EmbeddingService();
        VectorStoreService store = new VectorStoreService();
        IngestionService ingestion = new IngestionService(kb, parser, chunker, embedding, store);

        String text = "Alpha beta gamma. ".repeat(50);
        int count = ingestion.ingestText(kbObj.getId(), text, "unit-text", null);
        assertTrue(count > 1);
        assertEquals(count, store.getAll(kbObj.getId()).size());
    }
}

