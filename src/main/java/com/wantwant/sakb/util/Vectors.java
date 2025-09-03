package com.wantwant.sakb.util;

public final class Vectors {
    private Vectors() {}

    public static double cosine(float[] a, float[] b) {
        if (a == null || b == null) return -1;
        int len = Math.min(a.length, b.length);
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}

