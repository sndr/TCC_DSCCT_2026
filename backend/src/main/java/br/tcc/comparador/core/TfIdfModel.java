package br.tcc.comparador.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Modelo TF-IDF implementado do zero.
 *
 * TF  = 1 + ln(contagem)           (sublinear)
 * IDF = ln((1 + N) / (1 + df)) + 1 (suavizado, nunca zera termos comuns)
 *
 * O IDF é calculado sobre um corpus (aqui, os parágrafos dos documentos comparados).
 */
public final class TfIdfModel {

    private final Map<String, Double> idf = new HashMap<>();
    private final double defaultIdf;

    public TfIdfModel(List<List<String>> corpus) {
        int n = corpus.size();
        Map<String, Integer> documentFrequency = new HashMap<>();
        for (List<String> document : corpus) {
            for (String term : new HashSet<>(document)) {
                documentFrequency.merge(term, 1, Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> entry : documentFrequency.entrySet()) {
            idf.put(entry.getKey(), Math.log((1.0 + n) / (1.0 + entry.getValue())) + 1.0);
        }
        this.defaultIdf = Math.log(1.0 + n) + 1.0;
    }

    public Map<String, Double> vectorize(List<String> tokens) {
        Map<String, Integer> counts = new HashMap<>();
        for (String token : tokens) {
            counts.merge(token, 1, Integer::sum);
        }
        Map<String, Double> vector = new HashMap<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            double tf = 1.0 + Math.log(entry.getValue());
            vector.put(entry.getKey(), tf * idf.getOrDefault(entry.getKey(), defaultIdf));
        }
        return vector;
    }

    public static double cosine(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Map<String, Double> small = a.size() <= b.size() ? a : b;
        Map<String, Double> large = small == a ? b : a;
        double dot = 0.0;
        for (Map.Entry<String, Double> entry : small.entrySet()) {
            Double other = large.get(entry.getKey());
            if (other != null) {
                dot += entry.getValue() * other;
            }
        }
        double denominator = norm(a) * norm(b);
        return denominator == 0.0 ? 0.0 : clamp(dot / denominator);
    }

    public static Set<String> sharedTerms(Map<String, Double> a, Map<String, Double> b) {
        Set<String> shared = new HashSet<>(a.keySet());
        shared.retainAll(b.keySet());
        return shared;
    }

    private static double norm(Map<String, Double> vector) {
        double sum = 0.0;
        for (double value : vector.values()) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
