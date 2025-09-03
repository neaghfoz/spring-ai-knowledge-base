package com.wantwant.sakb.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkerServiceTest {

    @Test
    void chunk_splitsWithOverlap() {
        TextChunkerService svc = new TextChunkerService();
        String text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(20);
        List<String> chunks = svc.chunk(text, 200, 50);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() > 1);
        assertTrue(chunks.get(0).length() <= 200);
    }

    @Test
    void chunk_handlesEmpty() {
        TextChunkerService svc = new TextChunkerService();
        assertTrue(svc.chunk("", 100, 10).isEmpty());
    }
}

