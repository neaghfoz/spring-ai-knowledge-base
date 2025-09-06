package com.wantwant.sakb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wantwant.sakb.config.DifyProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
@ConditionalOnProperty(value = "dify.enabled", havingValue = "true")
public class DifyWorkflowService {
    private final WebClient dify;
    private final DifyProperties props;
    private final ObjectMapper om = new ObjectMapper();

    public DifyWorkflowService(@Qualifier("difyWebClient") WebClient dify, DifyProperties props) {
        this.dify = dify;
        this.props = props;
    }

    public static class WorkflowResult {
        public final String text;
        public final JsonNode raw;
        public WorkflowResult(String text, JsonNode raw) { this.text = text; this.raw = raw; }
    }

    /**
     * Run a Dify workflow in blocking mode and return a simplified result.
     * The API path defaults to /v1/workflows/run.
     */
    public WorkflowResult runBlocking(Map<String, Object> inputs, String user, String workflowId) {
        if (workflowId == null || workflowId.isBlank()) workflowId = props.getWorkflowId();
        Map<String, Object> payload = new HashMap<>();
        if (workflowId != null && !workflowId.isBlank()) payload.put("workflow_id", workflowId);
        payload.put("inputs", inputs == null ? Map.of() : inputs);
        payload.put("response_mode", "blocking");
        if (user != null && !user.isBlank()) payload.put("user", user);

        Mono<JsonNode> call = dify.post()
                .uri("/v1/workflows/run")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(payload))
                .retrieve()
                .bodyToMono(String.class)
                .map(s -> {
                    try { return om.readTree(s); } catch (Exception e) { throw new RuntimeException(e); }
                });

        JsonNode root = call.block();
        if (root == null) return new WorkflowResult("", null);
        // Heuristic extraction: prefer data.outputs.text or data.text, else flatten first string field
        String text = "";
        JsonNode data = root.path("data");
        if (data.has("outputs") && data.path("outputs").has("text")) {
            text = data.path("outputs").path("text").asText("");
        } else if (data.has("text")) {
            text = data.path("text").asText("");
        } else if (root.has("text")) {
            text = root.path("text").asText("");
        }
        return new WorkflowResult(text, root);
    }
}
