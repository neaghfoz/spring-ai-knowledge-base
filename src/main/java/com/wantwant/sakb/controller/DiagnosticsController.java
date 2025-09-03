package com.wantwant.sakb.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/diagnostics")
public class DiagnosticsController {

    private final ObjectProvider<ChatClient> chatClientProvider;
    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final Environment env;

    public DiagnosticsController(ObjectProvider<ChatClient> chatClientProvider,
                                 ObjectProvider<ChatModel> chatModelProvider,
                                 ObjectProvider<EmbeddingModel> embeddingModelProvider,
                                 Environment env) {
        this.chatClientProvider = chatClientProvider;
        this.chatModelProvider = chatModelProvider;
        this.embeddingModelProvider = embeddingModelProvider;
        this.env = env;
    }

    @GetMapping("/ai")
    public ResponseEntity<Map<String, Object>> ai() {
        Map<String, Object> out = new HashMap<>();
        out.put("chatClientAvailable", chatClientProvider.getIfAvailable() != null);
        out.put("chatModelAvailable", chatModelProvider.getIfAvailable() != null);
        out.put("embeddingModelAvailable", embeddingModelProvider.getIfAvailable() != null);
        out.put("ollamaBaseUrl", env.getProperty("spring.ai.ollama.base-url"));
        out.put("ollamaChatModel", env.getProperty("spring.ai.ollama.chat.options.model"));
        out.put("ollamaEmbeddingModel", env.getProperty("spring.ai.ollama.embedding.options.model"));
        out.put("ollamaEnabled", env.getProperty("spring.ai.ollama.enabled"));
        out.put("ollamaChatEnabled", env.getProperty("spring.ai.ollama.chat.enabled"));
        out.put("ollamaEmbeddingEnabled", env.getProperty("spring.ai.ollama.embedding.enabled"));
        out.put("openaiEnabled", env.getProperty("spring.ai.openai.enabled"));
        return ResponseEntity.ok(out);
    }
}

