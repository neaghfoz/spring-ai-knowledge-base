package com.wantwant.sakb.config;

import java.util.List;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.wantwant.sakb.service.Embeddings;

@Configuration
public class AiEmbeddingConfig {

    @Bean
    @Primary
    @ConditionalOnBean(EmbeddingModel.class)
    public Embeddings providerEmbeddings(EmbeddingModel model) {
        return text -> {
            if (text == null || text.isBlank()) return new float[0];
            var response = model.embedForResponse(List.of(text));
            var data = response.getResults();
            if (data == null || data.isEmpty()) return new float[0];
            // Spring AI 1.x Embedding output is float[]
            float[] vec = data.get(0).getOutput();
            return vec == null ? new float[0] : vec;
        };
    }
}
