package com.ohcp.agent.knowledge;

/**
 * 轻量级中文文本向量化器。
 * 基于字符一元/二元 n-gram + 哈希技巧构建稀疏向量，用于本地余弦相似度检索，
 * 无需依赖任何外部向量服务，保证离线可演示。
 */
public final class Vectorizer {

    public static final int DIM = 768;

    public double[] vectorize(String text) {
        double[] vector = new double[DIM];
        if (text == null || text.isBlank()) {
            return vector;
        }
        String normalized = normalize(text);
        // 单字特征
        for (int i = 0; i < normalized.length(); i++) {
            add(vector, normalized.substring(i, i + 1), 0.6);
        }
        // 双字特征（对中文词组更敏感）
        for (int i = 0; i + 1 < normalized.length(); i++) {
            add(vector, normalized.substring(i, i + 2), 1.0);
        }
        l2Normalize(vector);
        return vector;
    }

    private void add(double[] vector, String token, double weight) {
        int hash = token.hashCode();
        int index = (hash & Integer.MAX_VALUE) % DIM;
        vector[index] += weight;
    }

    private String normalize(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c) || Character.isIdeographic(c)) {
                sb.append(c);
            }
        }
        return sb.toString().toLowerCase();
    }

    private void l2Normalize(double[] vector) {
        double norm = 0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }
    }

    public static double cosine(double[] a, double[] b) {
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}