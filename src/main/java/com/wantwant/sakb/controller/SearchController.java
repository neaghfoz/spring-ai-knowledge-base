package com.wantwant.sakb.controller;

import com.wantwant.sakb.dto.SearchRequest;
import com.wantwant.sakb.model.DocumentChunk;
import com.wantwant.sakb.service.Embeddings;
import com.wantwant.sakb.service.KnowledgeBaseService;
import com.wantwant.sakb.service.VectorStore;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final KnowledgeBaseService kbService;
    private final Embeddings embeddings;
    private final VectorStore vectorStoreService;

    public SearchController(KnowledgeBaseService kbService,
                            Embeddings embeddings,
                            VectorStore vectorStoreService) {
        this.kbService = kbService;
        this.embeddings = embeddings;
        this.vectorStoreService = vectorStoreService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> search(@Valid @RequestBody SearchRequest req) {
        kbService.get(req.getKbId()).orElseThrow(() -> new com.wantwant.sakb.exception.NotFoundException("KnowledgeBase not found: " + req.getKbId()));
        float[] q = embeddings.embed(req.getQuery());
        var hits = vectorStoreService.search(req.getKbId(), q, req.getTopK());
        List<Map<String, Object>> items = hits.stream().map(h -> {
            DocumentChunk c = h.item();
            Map<String, Object> m = new HashMap<>();
            m.put("sourceName", c.getSourceName());
            m.put("chunkIndex", c.getChunkIndex());
            m.put("score", h.score());
            m.put("text", c.getText());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> body = new HashMap<>();
        body.put("items", items);
        return ResponseEntity.ok(body);
    }
}
