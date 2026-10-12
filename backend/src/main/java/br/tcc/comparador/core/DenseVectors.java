package br.tcc.comparador.core;

import java.util.List;

/** Operações sobre vetores densos (embeddings). */
public final class DenseVectors {

    private DenseVectors() {
    }

    public static double cosine(float[] a, float[] b) {
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        int length = Math.min(a.length, b.length);
        for (int i = 0; i < length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        double value = dot / (Math.sqrt(normA) * Math.sqrt(normB));
        return Math.max(0.0, Math.min(1.0, value));
    }

    /** Média dos vetores (representação do documento a partir dos parágrafos). */
    public static float[] mean(List<float[]> vectors) {
        if (vectors.isEmpty()) {
            return new float[0];
        }
        float[] result = new float[vectors.get(0).length];
        for (float[] vector : vectors) {
            for (int i = 0; i < result.length; i++) {
                result[i] += vector[i];
            }
        }
        for (int i = 0; i < result.length; i++) {
            result[i] /= vectors.size();
        }
        return result;
    }
}
