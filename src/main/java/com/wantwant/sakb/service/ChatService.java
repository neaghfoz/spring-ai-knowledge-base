package com.wantwant.sakb.service;

import com.wantwant.sakb.exception.NotFoundException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    public static class ChatAnswer {
        public final String answer;
        public final List<Source> sources;
        public ChatAnswer(String answer, List<Source> sources) {
            this.answer = answer;
            this.sources = sources;
        }
    }

    public static class Source {
        public final String sourceName;
        public final int chunkIndex;
        public final double score;
        public final String text;
        public Source(String sourceName, int chunkIndex, double score, String text) {
            this.sourceName = sourceName;
            this.chunkIndex = chunkIndex;
            this.score = score;
            this.text = text;
        }
    }

    private final KnowledgeBaseService kbService;
    private final Embeddings embeddings;
    private final VectorStore vectorStore;
    private final ConversationMemoryService memoryService;
    private final ChatClient chatClient; // may be null

    public ChatService(KnowledgeBaseService kbService,
                       Embeddings embeddings,
                       VectorStore vectorStore,
                       ConversationMemoryService memoryService,
                       ObjectProvider<ChatClient> chatClientProvider) {
        this.kbService = kbService;
        this.embeddings = embeddings;
        this.vectorStore = vectorStore;
        this.memoryService = memoryService;
        this.chatClient = chatClientProvider.getIfAvailable();
    }

    public ChatAnswer chat(String kbId, String question, int topK, String sessionId) {
        kbService.get(kbId).orElseThrow(() -> new NotFoundException("KnowledgeBase not found: " + kbId));
        float[] q = embeddings.embed(question);
        var hits = vectorStore.search(kbId, q, Math.max(1, topK));
        var sources = hits.stream()
                .map(h -> new Source(h.item().getSourceName(), h.item().getChunkIndex(), h.score(), h.item().getText()))
                .collect(Collectors.toList());
        String context = hits.stream().map(h -> h.item().getText()).collect(Collectors.joining("\n---\n"));
        String history = buildHistory(sessionId);
        String prompt = "You are a helpful assistant. Use only the provided context and prior turns to answer.\n" +
                "If the answer cannot be found, say you don't know.\n\n" +
                (history.isBlank() ? "" : ("Conversation History:\n" + history + "\n\n")) +
                "Context:\n" + context + "\n\nQuestion: " + question + "\nAnswer:";
        String answer;
        if (chatClient != null) {
            answer = chatClient.prompt().system(prompt).call().content();
        } else {
            answer = fallbackAnswer(context, question, history);
        }
        if (sessionId != null && !sessionId.isBlank()) {
            memoryService.append(sessionId, question, answer);
        }
        return new ChatAnswer(answer, sources);
    }

    private String buildHistory(String sessionId) {
        var turns = memoryService.history(sessionId);
        if (turns.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (var t : turns) {
            sb.append(t.role()).append(": ").append(t.text()).append('\n');
        }
        return sb.toString();
    }

    private String fallbackAnswer(String context, String question, String history) {
        String combined = (history == null ? "" : history) + "\n" + (context == null ? "" : context);
        if (combined.isBlank()) return "I don't know based on the current knowledge base.";
        String snippet = combined.length() > 600 ? combined.substring(0, 600) + "..." : combined;
        return "(Local fallback) Based on KB and history: \n" + snippet;
    }
}
