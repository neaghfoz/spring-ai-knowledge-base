package com.wantwant.sakb.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmbeddingService implements Embeddings {
    private static final int DIM = 256;

    @Override
    public float[] embed(String text) {
        if (text == null) return new float[DIM];
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        float[] vec = new float[DIM];
        for (int i = 0; i < bytes.length; i++) {
            int idx = i % DIM;
            vec[idx] += (bytes[i] & 0xff) / 255.0f;
        }
        normalize(vec);
        return vec;
    }

    private void normalize(float[] v) {
        double norm = 0;
        for (float x : v) norm += x * x;
        norm = Math.sqrt(norm);
        if (norm == 0) return;
        for (int i = 0; i < v.length; i++) v[i] /= (float) norm;
    }
}
