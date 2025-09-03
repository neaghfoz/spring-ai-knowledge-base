package com.wantwant.sakb.controller;

import com.wantwant.sakb.dto.CreateKbRequest;
import com.wantwant.sakb.model.KnowledgeBase;
import com.wantwant.sakb.service.KnowledgeBaseService;
import com.wantwant.sakb.service.VectorStore;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/kb")
public class KnowledgeBaseController {

    private final KnowledgeBaseService kbService;
    private final VectorStore vectorStoreService;

    public KnowledgeBaseController(KnowledgeBaseService kbService, VectorStore vectorStoreService) {
        this.kbService = kbService;
        this.vectorStoreService = vectorStoreService;
    }

    @PostMapping
    public ResponseEntity<KnowledgeBase> create(@Valid @RequestBody CreateKbRequest req) {
        KnowledgeBase kb = kbService.create(req.getName(), req.getDescription());
        return ResponseEntity.created(URI.create("/api/kb/" + kb.getId())).body(kb);
    }

    @GetMapping
    public List<KnowledgeBase> list() {
        return kbService.list();
    }

    @GetMapping("/{id}")
    public KnowledgeBase get(@PathVariable String id) {
        return kbService.get(id).orElseThrow(() -> new com.wantwant.sakb.exception.NotFoundException("KnowledgeBase not found: " + id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        kbService.delete(id);
        vectorStoreService.deleteKb(id);
        return ResponseEntity.noContent().build();
    }
}
