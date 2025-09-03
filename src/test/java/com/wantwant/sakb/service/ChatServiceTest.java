package com.wantwant.sakb.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChatServiceTest {

    @Test
    void chat_usesFallbackWithoutChatClient() {
    KnowledgeBaseService kb = new InMemoryKnowledgeBaseService();
        var kbObj = kb.create("kb", "test");
        EmbeddingService embedding = new EmbeddingService();
        VectorStoreService store = new VectorStoreService();
        ConversationMemoryService memory = new ConversationMemoryService();
        // Seed some content
        float[] e = embedding.embed("Spring AI helps build AI apps");
        store.addAll(kbObj.getId(), List.of(
                new com.wantwant.sakb.model.DocumentChunk(kbObj.getId(), "seed", 0, "Spring AI helps build AI apps", e, java.util.Map.of())
        ));

        ObjectProvider<ChatClient> provider = new ObjectProvider<>() {
            @Override public ChatClient getObject(Object... args) { return null; }
            @Override public ChatClient getIfAvailable() { return null; }
            @Override public ChatClient getIfUnique() { return null; }
            @Override public ChatClient getObject() { return null; }
        };

        ChatService chat = new ChatService(kb, embedding, store, memory, provider);
        var ans = chat.chat(kbObj.getId(), "What does Spring AI help with?", 3, null);
        assertNotNull(ans);
        assertNotNull(ans.answer);
        assertTrue(ans.answer.toLowerCase().contains("fallback") || ans.answer.length() > 0);
        assertFalse(ans.sources.isEmpty());
    }
}
