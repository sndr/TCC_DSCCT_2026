package br.tcc.comparador.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Palavras (termos distintos, sem stopwords) presentes nos dois documentos. */
public final class LexicalOverlap {

    public record Overlap(int total, List<String> words) {
    }

    private LexicalOverlap() {
    }

    public static Overlap shared(List<String> tokensA, List<String> tokensB, int limit) {
        Map<String, Integer> countsA = count(tokensA);
        Map<String, Integer> countsB = count(tokensB);
        List<String> shared = new ArrayList<>();
        for (String term : countsA.keySet()) {
            if (countsB.containsKey(term)) {
                shared.add(term);
            }
        }
        shared.sort(Comparator
            .comparingInt((String t) -> Math.min(countsA.get(t), countsB.get(t))).reversed()
            .thenComparing(Comparator.naturalOrder()));
        List<String> top = shared.size() > limit ? new ArrayList<>(shared.subList(0, limit)) : shared;
        return new Overlap(shared.size(), top);
    }

    private static Map<String, Integer> count(List<String> tokens) {
        Map<String, Integer> counts = new HashMap<>();
        for (String token : tokens) {
            counts.merge(token, 1, Integer::sum);
        }
        return counts;
    }
}
