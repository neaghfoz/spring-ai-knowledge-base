package com.wantwant.sakb.controller;

import com.wantwant.sakb.service.DifyKbService;
import com.wantwant.sakb.service.DifyWorkflowService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dify")
@ConditionalOnProperty(value = "dify.enabled", havingValue = "true")
public class DifyController {

    private final DifyWorkflowService workflowService;
    private final DifyKbService kbService;

    public DifyController(DifyWorkflowService workflowService, DifyKbService kbService) {
        this.workflowService = workflowService;
        this.kbService = kbService;
    }

    // ===== Workflow =====
    @PostMapping("/workflow/run")
    public ResponseEntity<Map<String, Object>> runWorkflow(@RequestBody Map<String, Object> body) {
        String user = body.getOrDefault("user", "user").toString();
        String workflowId = body.get("workflowId") == null ? null : body.get("workflowId").toString();
        @SuppressWarnings("unchecked")
        Map<String, Object> inputs = (Map<String, Object>) body.getOrDefault("inputs", new HashMap<>());
        var res = workflowService.runBlocking(inputs, user, workflowId);
        Map<String, Object> out = new HashMap<>();
        out.put("text", res.text);
        out.put("raw", res.raw);
        return ResponseEntity.ok(out);
    }

    // ===== Knowledge base (Dataset) search =====
    @PostMapping("/kb/search")
    public ResponseEntity<List<DifyKbService.SearchHit>> search(@RequestBody Map<String, Object> body) {
        String query = body.getOrDefault("query", "").toString();
        Integer topK = body.get("topK") == null ? null : Integer.valueOf(body.get("topK").toString());
        String datasetId = body.get("datasetId") == null ? null : body.get("datasetId").toString();
        return ResponseEntity.ok(kbService.search(query, topK, datasetId));
    }

    // Upsert plain text into dataset
    @PostMapping("/kb/documents")
    public ResponseEntity<Map<String, Object>> upsertText(@RequestBody Map<String, Object> body) {
        String datasetId = body.get("datasetId") == null ? null : body.get("datasetId").toString();
        String title = body.getOrDefault("title", "Untitled").toString();
        String text = body.getOrDefault("text", "").toString();
        @SuppressWarnings("unchecked") Map<String, Object> meta = (Map<String, Object>) body.getOrDefault("metadata", Map.of());
        String id = kbService.upsertText(datasetId, title, text, meta);
        return ResponseEntity.ok(Map.of("id", id));
    }

    // Upload a file into dataset
    @PostMapping(value = "/kb/document-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(@RequestPart("file") MultipartFile file,
                                                      @RequestParam(value = "datasetId", required = false) String datasetId) throws Exception {
        File tmp = File.createTempFile("dify-upload-", "-tmp");
        file.transferTo(tmp);
        try {
            String id = kbService.uploadFile(datasetId, tmp, Map.of("filename", file.getOriginalFilename()));
            return ResponseEntity.ok(Map.of("id", id));
        } finally {
            // best-effort cleanup
            //noinspection ResultOfMethodCallIgnored
            tmp.delete();
        }
    }

    @DeleteMapping("/kb/documents/{id}")
    public ResponseEntity<Void> deleteDoc(@PathVariable("id") @NotBlank String docId,
                                          @RequestParam(value = "datasetId", required = false) String datasetId) {
        kbService.deleteDocument(datasetId, docId);
        return ResponseEntity.noContent().build();
    }
}

