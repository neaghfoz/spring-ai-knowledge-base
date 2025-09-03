package com.wantwant.sakb.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmbeddingServiceTest {

    @Test
    void embed_returnsNormalizedVector() {
        EmbeddingService svc = new EmbeddingService();
        float[] v = svc.embed("hello world");
        assertEquals(256, v.length);
        double norm = 0;
        for (float x : v) norm += x * x;
        norm = Math.sqrt(norm);
        assertTrue(norm > 0.99 && norm < 1.01, "Vector should be ~unit length");
    }

    @Test
    void embed_sameTextSimilar() {
        EmbeddingService svc = new EmbeddingService();
        float[] a = svc.embed("abc");
        float[] b = svc.embed("abc");
        double dot = 0;
        for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        assertTrue(dot > 0.95);
    }
}

