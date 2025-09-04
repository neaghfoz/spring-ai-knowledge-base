package com.wantwant.sakb.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.wantwant.sakb.dto.IngestTextRequest;
import com.wantwant.sakb.dto.UpsertChunkRequest;
import com.wantwant.sakb.model.DocumentChunk;
import com.wantwant.sakb.service.IngestionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ingest")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping(path = "/text", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> ingestText(@Valid @RequestBody IngestTextRequest req) throws Exception {
        int count = ingestionService.ingestText(req.getKbId(), req.getText(), req.getSourceName(), req.getMetadata());
        Map<String, Object> body = new HashMap<>();
        body.put("ingestedChunks", count);
        return ResponseEntity.ok(body);
    }

    @PostMapping(path = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> ingestFile(@RequestParam("kbId") String kbId,
                                                          @RequestPart("file") MultipartFile file) throws Exception {
        int count = ingestionService.ingestFile(kbId, file);
        Map<String, Object> body = new HashMap<>();
        body.put("ingestedChunks", count);
        body.put("filename", file.getOriginalFilename());
        return ResponseEntity.ok(body);
    }

    // ===== Management endpoints =====

    @GetMapping(path = "/sources")
    public ResponseEntity<Map<String, Object>> listSources(@RequestParam("kbId") String kbId) {
        List<String> sources = ingestionService.listSources(kbId);
        List<Map<String, Object>> items = sources.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", s);
            m.put("count", ingestionService.countChunks(kbId, s));
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> body = new HashMap<>();
        body.put("sources", items);
        return ResponseEntity.ok(body);
    }

    @GetMapping(path = "/chunks")
    public ResponseEntity<Map<String, Object>> listChunks(@RequestParam("kbId") String kbId,
                                                          @RequestParam("sourceName") String sourceName) {
        List<DocumentChunk> chunks = ingestionService.listChunks(kbId, sourceName);
        List<Map<String, Object>> items = chunks.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("sourceName", c.getSourceName());
            m.put("chunkIndex", c.getChunkIndex());
            m.put("text", c.getText());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> body = new HashMap<>();
        body.put("items", items);
        return ResponseEntity.ok(body);
    }

    @GetMapping(path = "/count")
    public ResponseEntity<Map<String, Object>> count(@RequestParam("kbId") String kbId,
                                                     @RequestParam("sourceName") String sourceName) {
        int n = ingestionService.countChunks(kbId, sourceName);
        Map<String, Object> body = new HashMap<>();
        body.put("kbId", kbId);
        body.put("sourceName", sourceName);
        body.put("count", n);
        return ResponseEntity.ok(body);
    }

    @DeleteMapping(path = "/source")
    public ResponseEntity<Void> deleteSource(@RequestParam("kbId") String kbId,
                                             @RequestParam("sourceName") String sourceName) {
        ingestionService.deleteSource(kbId, sourceName);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(path = "/chunk")
    public ResponseEntity<Void> deleteChunk(@RequestParam("kbId") String kbId,
                                            @RequestParam("sourceName") String sourceName,
                                            @RequestParam("chunkIndex") int chunkIndex) {
        ingestionService.deleteChunk(kbId, sourceName, chunkIndex);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(path = "/chunk", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> upsertChunk(@Valid @RequestBody UpsertChunkRequest req) {
        ingestionService.upsertChunk(req.getKbId(), req.getSourceName(), req.getChunkIndex(), req.getText(), req.getMetadata());
        Map<String, Object> body = new HashMap<>();
        body.put("status", "ok");
        body.put("kbId", req.getKbId());
        body.put("sourceName", req.getSourceName());
        body.put("chunkIndex", req.getChunkIndex());
        return ResponseEntity.ok(body);
    }
}
