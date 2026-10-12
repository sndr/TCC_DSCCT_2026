package br.tcc.comparador.service;

import br.tcc.comparador.config.AppProperties;
import br.tcc.comparador.core.LexicalOverlap.Overlap;
import br.tcc.comparador.core.ParagraphMatcher;
import br.tcc.comparador.core.ParagraphMatcher.Match;
import br.tcc.comparador.core.TfIdfModel;
import br.tcc.comparador.web.Dtos.ParagrafoRelacionado;
import br.tcc.comparador.web.Dtos.ResultadoMetodo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TfIdfSimilaridadeService {

    public static final String NOME = "TF-IDF";
    static final int MAX_PARAGRAFOS_EXIBIDOS = 10;

    private final double limiarParagrafo;

    public TfIdfSimilaridadeService(AppProperties properties) {
        this.limiarParagrafo = properties.similarity().tfidfParagraphThreshold();
    }

    public ResultadoMetodo comparar(DocumentoPreparado a, DocumentoPreparado b, Overlap overlap) {
        long inicio = System.nanoTime();

        List<List<String>> corpus = new ArrayList<>(a.tokensParagrafos());
        corpus.addAll(b.tokensParagrafos());
        if (corpus.size() < 2) {
            corpus.add(a.tokens());
            corpus.add(b.tokens());
        }
        TfIdfModel modelo = new TfIdfModel(corpus);

        double similaridade = TfIdfModel.cosine(modelo.vectorize(a.tokens()), modelo.vectorize(b.tokens()));

        List<Map<String, Double>> vetoresA = a.tokensParagrafos().stream().map(modelo::vectorize).toList();
        List<Map<String, Double>> vetoresB = b.tokensParagrafos().stream().map(modelo::vectorize).toList();
        List<Match> pares = ParagraphMatcher.match(vetoresA.size(), vetoresB.size(),
            (i, j) -> TfIdfModel.cosine(vetoresA.get(i), vetoresB.get(j)), limiarParagrafo);

        List<ParagrafoRelacionado> exibidos = pares.stream()
            .limit(MAX_PARAGRAFOS_EXIBIDOS)
            .map(m -> new ParagrafoRelacionado(a.paragrafos().get(m.indexA()), b.paragrafos().get(m.indexB()), m.score()))
            .toList();

        double tempoMs = (System.nanoTime() - inicio) / 1_000_000.0;
        return new ResultadoMetodo(NOME, similaridade, overlap.total(), pares.size(),
            arredondar(tempoMs), overlap.words(), exibidos, null);
    }

    static double arredondar(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
