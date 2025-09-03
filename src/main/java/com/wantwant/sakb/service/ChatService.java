package com.wantwant.sakb.service;

import com.wantwant.sakb.exception.NotFoundException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

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
    private final ObjectProvider<ChatClient> chatClientProvider;
    private final ObjectProvider<ChatModel> chatModelProvider;

    public ChatService(KnowledgeBaseService kbService,
                       Embeddings embeddings,
                       VectorStore vectorStore,
                       ConversationMemoryService memoryService,
                       ObjectProvider<ChatClient> chatClientProvider,
                       ObjectProvider<ChatModel> chatModelProvider) {
        this.kbService = kbService;
        this.embeddings = embeddings;
        this.vectorStore = vectorStore;
        this.memoryService = memoryService;
        this.chatClientProvider = chatClientProvider;
        this.chatModelProvider = chatModelProvider;
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
        String prompt = "你是一个有帮助的助手。请仅使用提供的上下文和先前对话进行回答。\n" +
                "如果无法从上下文中找到答案，请直接说明你不知道。\n" +
                "所有回答必须使用简体中文。\n\n" +
                (history.isBlank() ? "" : ("��话历史：\n" + history + "\n\n")) +
                "上下文：\n" + context + "\n\n问题：" + question + "\n回答：";
        ChatClient chatClient = chatClientProvider.getIfAvailable();
        if (chatClient == null) {
            ChatModel model = chatModelProvider.getIfAvailable();
            if (model != null) {
                chatClient = ChatClient.create(model);
                log.debug("Constructed ChatClient on the fly from ChatModel.");
            }
        }
        String answer;
        if (chatClient == null) {
            log.debug("Using local fallback to generate answer (no ChatClient).");
            answer = fallbackAnswer(context, question, history);
        } else {
            try {
                log.debug("Generating answer using ChatClient (LLM provider).");
                answer = chatClient.prompt().system(prompt).call().content();
            } catch (Exception ex) {
                log.warn("Chat provider failed; falling back to local mode: {}", ex.toString());
                answer = fallbackAnswer(context, question, history);
            }
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
        if (combined.isBlank()) return "基于当前知识库，我无法回答。";
        String snippet = combined.length() > 600 ? combined.substring(0, 600) + "..." : combined;
        String qText = (question == null || question.isBlank()) ? "（未提供）" : question;
        return "（本地回退）针对问题：" + qText + "\n根据知识库与对话历史：\n" + snippet;
    }
}
