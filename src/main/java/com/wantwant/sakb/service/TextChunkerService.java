package com.wantwant.sakb.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TextChunkerService {

    private static final int DEFAULT_CHUNK_SIZE = 800; // chars
    private static final int DEFAULT_OVERLAP = 100;    // chars
    private static final String TABLE_ROW_MARK = "##[TABLE_ROW]";

    private int tableRowsPerChunk = 1;

    @Value("${kb.table.rowsPerChunk:1}")
    public void setTableRowsPerChunk(int v) {
        this.tableRowsPerChunk = Math.max(1, v);
    }

    public List<String> chunk(String text) {
        if (text != null && text.contains(TABLE_ROW_MARK)) {
            return chunkTableRows(text, tableRowsPerChunk);
        }
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

    private List<String> chunkTableRows(String text, int rowsPerChunk) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            if (line.startsWith(TABLE_ROW_MARK)) lines.add(line);
        }
        if (lines.isEmpty()) return chunk(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
        List<String> chunks = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String l : lines) {
            if (count == 0) sb.append("表格记录：\n");
            sb.append(l).append('\n');
            count++;
            if (count >= rowsPerChunk) {
                chunks.add(sb.toString().trim());
                sb.setLength(0);
                count = 0;
            }
        }
        if (sb.length() > 0) chunks.add(sb.toString().trim());
        return chunks;
    }
}
