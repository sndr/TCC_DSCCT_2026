package br.tcc.comparador.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Encontra parágrafos relacionados entre dois documentos.
 * Para cada parágrafo de A, procura o melhor correspondente em B.
 */
public final class ParagraphMatcher {

    @FunctionalInterface
    public interface PairScorer {
        double score(int indexA, int indexB);
    }

    public record Match(int indexA, int indexB, double score) {
    }

    private ParagraphMatcher() {
    }

    public static List<Match> match(int sizeA, int sizeB, PairScorer scorer, double threshold) {
        List<Match> matches = new ArrayList<>();
        for (int i = 0; i < sizeA; i++) {
            int bestIndex = -1;
            double bestScore = 0.0;
            for (int j = 0; j < sizeB; j++) {
                double score = scorer.score(i, j);
                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = j;
                }
            }
            if (bestIndex >= 0 && bestScore >= threshold) {
                matches.add(new Match(i, bestIndex, bestScore));
            }
        }
        matches.sort(Comparator.comparingDouble(Match::score).reversed());
        return matches;
    }
}
