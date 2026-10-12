package br.tcc.comparador.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NucleoTest {

    private static double tfidf(String a, String b) {
        List<List<String>> corpus = new ArrayList<>();
        for (String p : ParagraphSplitter.split(a)) corpus.add(TextPreprocessor.tokenize(p));
        for (String p : ParagraphSplitter.split(b)) corpus.add(TextPreprocessor.tokenize(p));
        TfIdfModel modelo = new TfIdfModel(corpus);
        return TfIdfModel.cosine(
            modelo.vectorize(TextPreprocessor.tokenize(a)),
            modelo.vectorize(TextPreprocessor.tokenize(b)));
    }

    @Test
    void documentosIdenticosTemSimilaridadeUm() {
        String texto = "Aprendizado de máquina usa dados para treinar modelos.\n\nRedes neurais reconhecem padrões em imagens.";
        assertEquals(1.0, tfidf(texto, texto), 1e-9);
    }

    @Test
    void documentosDeTemasDiferentesTemSimilaridadeZero() {
        String a = "Aprendizado de máquina usa dados para treinar modelos de classificação.";
        String b = "A receita de bolo leva cenoura, ovos, açúcar e chocolate na cobertura.";
        assertEquals(0.0, tfidf(a, b), 1e-9);
    }

    @Test
    void tokenizacaoRemoveAcentosEStopwords() {
        assertEquals(List.of("acao", "rapida", "joao", "festa"),
            TextPreprocessor.tokenize("Ação rápida: o São João não é só festa!"));
    }

    @Test
    void sobreposicaoLexicalContaTermosDistintos() {
        var overlap = LexicalOverlap.shared(List.of("gato", "gato", "cachorro"), List.of("gato", "peixe"), 10);
        assertEquals(1, overlap.total());
        assertTrue(overlap.words().contains("gato"));
    }
}
