package br.tcc.comparador.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(List<String> corsOrigins, Embeddings embeddings, Similarity similarity) {

    public record Embeddings(boolean enabled, String model) {
    }

    public record Similarity(double tfidfParagraphThreshold, double embeddingsParagraphThreshold) {
    }
}
