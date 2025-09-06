package com.wantwant.sakb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wantwant.sakb.config.DifyProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.util.*;

@Service
@ConditionalOnProperty(value = "dify.enabled", havingValue = "true")
public class DifyKbService {
    private final WebClient dify;
    private final DifyProperties props;
    private final ObjectMapper om = new ObjectMapper();

    public DifyKbService(@Qualifier("difyWebClient") WebClient dify, DifyProperties props) {
        this.dify = dify;
        this.props = props;
    }

    public static class SearchHit {
        public final String id;
        public final String text;
        public final double score;
        public final Map<String, Object> metadata;
        public SearchHit(String id, String text, double score, Map<String, Object> metadata) {
            this.id = id; this.text = text; this.score = score; this.metadata = metadata;
        }
    }

    public List<SearchHit> search(String query, Integer topK, String datasetId) {
        if (datasetId == null || datasetId.isBlank()) datasetId = props.getDatasetId();
        Map<String, Object> payload = new HashMap<>();
        payload.put("query", query);
        payload.put("datasets", List.of(datasetId));
        if (topK != null) payload.put("top_k", topK);
        String json = dify.post()
                .uri("/v1/retrievals")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(payload))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        try {
            JsonNode root = om.readTree(json);
            List<SearchHit> out = new ArrayList<>();
            JsonNode items = root.path("data");
            if (items.isMissingNode()) items = root.path("hits");
            for (JsonNode n : items) {
                String id = n.path("id").asText("");
                String text = n.path("text").asText("");
                double score = n.path("score").asDouble(0.0);
                Map<String, Object> meta = new HashMap<>();
                if (n.has("metadata")) {
                    n.get("metadata").fields().forEachRemaining(e -> meta.put(e.getKey(), e.getValue().asText()));
                }
                out.add(new SearchHit(id, text, score, meta));
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Dify retrieval response", e);
        }
    }

    public String upsertText(String datasetId, String title, String text, Map<String, Object> metadata) {
        if (datasetId == null || datasetId.isBlank()) datasetId = props.getDatasetId();
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title == null ? "Untitled" : title);
        payload.put("text", text == null ? "" : text);
        if (metadata != null && !metadata.isEmpty()) payload.put("metadata", metadata);
        String json = dify.post()
                .uri("/v1/datasets/" + datasetId + "/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(payload))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        try {
            JsonNode root = om.readTree(json);
            return root.path("id").asText("");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Dify upsertText response", e);
        }
    }

    public String uploadFile(String datasetId, File file, Map<String, Object> metadata) {
        if (datasetId == null || datasetId.isBlank()) datasetId = props.getDatasetId();
        MultipartBodyBuilder mb = new MultipartBodyBuilder();
        mb.part("file", new FileSystemResource(file));
        if (metadata != null && !metadata.isEmpty()) {
            mb.part("metadata", metadata.toString());
        }
        MultiValueMap<String, org.springframework.http.HttpEntity<?>> body = mb.build();
        String json = dify.post()
                .uri("/v1/datasets/" + datasetId + "/document-files")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        try {
            JsonNode root = om.readTree(json);
            return root.path("id").asText("");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Dify uploadFile response", e);
        }
    }

    public void deleteDocument(String datasetId, String documentId) {
        if (datasetId == null || datasetId.isBlank()) datasetId = props.getDatasetId();
        dify.delete()
                .uri("/v1/datasets/" + datasetId + "/documents/" + documentId)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
