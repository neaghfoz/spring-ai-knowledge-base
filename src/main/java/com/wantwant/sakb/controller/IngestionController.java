package com.wantwant.sakb.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wantwant.sakb.dto.IngestTextRequest;
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
}

