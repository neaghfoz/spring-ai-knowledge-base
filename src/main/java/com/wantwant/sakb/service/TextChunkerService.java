package com.wantwant.sakb.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkerService {

    private static final int DEFAULT_CHUNK_SIZE = 800; // chars
    private static final int DEFAULT_OVERLAP = 100;    // chars

    public List<String> chunk(String text) {
        return chunk(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
        }

    public List<String> chunk(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;
        int start = 0;
        int len = text.length();
        while (start < len) {
            int end = Math.min(start + chunkSize, len);
            String chunk = text.substring(start, end).trim();
            if (!chunk.isBlank()) chunks.add(chunk);
            if (end == len) break;
            start = end - Math.min(overlap, end - start);
        }
        return chunks;
    }
}

