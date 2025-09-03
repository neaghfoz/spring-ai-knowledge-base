package com.wantwant.sakb.controller;

import com.wantwant.sakb.service.ChatService;
import com.wantwant.sakb.dto.ChatRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(@Valid @RequestBody ChatRequest req) {
        var ans = chatService.chat(req.getKbId(), req.getQuestion(), req.getTopK(), req.getSessionId());
        Map<String, Object> body = new HashMap<>();
        body.put("answer", ans.answer);
        body.put("sources", ans.sources);
        return ResponseEntity.ok(body);
    }

    @PostMapping(value = "/stream", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest req) {
        SseEmitter emitter = new SseEmitter(0L);
        new Thread(() -> {
            try {
                var ans = chatService.chat(req.getKbId(), req.getQuestion(), req.getTopK(), req.getSessionId());
                String text = ans.answer == null ? "" : ans.answer;
                int chunkSize = 64;
                for (int i = 0; i < text.length(); i += chunkSize) {
                    String chunk = text.substring(i, Math.min(i + chunkSize, text.length()));
                    emitter.send(SseEmitter.event().data(chunk));
                }
                emitter.send(SseEmitter.event().name("[DONE]").data(""));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }).start();
        return emitter;
    }
}
