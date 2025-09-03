package com.wantwant.sakb.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationMemoryService {
    public record Turn(String role, String text) {}

    private final Map<String, Deque<Turn>> sessions = new ConcurrentHashMap<>();
    private final int maxTurns = 20; // configurable if needed

    public List<Turn> history(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return List.of();
        return new ArrayList<>(sessions.getOrDefault(sessionId, new ArrayDeque<>()));
    }

    public void append(String sessionId, String userMsg, String assistantMsg) {
        if (sessionId == null || sessionId.isBlank()) return;
        Deque<Turn> dq = sessions.computeIfAbsent(sessionId, k -> new ArrayDeque<>());
        dq.addLast(new Turn("user", userMsg));
        dq.addLast(new Turn("assistant", assistantMsg));
        while (dq.size() > maxTurns) dq.removeFirst();
    }
}

