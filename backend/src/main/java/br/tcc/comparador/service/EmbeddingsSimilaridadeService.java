package br.tcc.comparador.service;

import br.tcc.comparador.config.AppProperties;
import br.tcc.comparador.core.DenseVectors;
import br.tcc.comparador.core.LexicalOverlap.Overlap;
import br.tcc.comparador.core.ParagraphMatcher;
import br.tcc.comparador.core.ParagraphMatcher.Match;
import br.tcc.comparador.web.Dtos.ParagrafoRelacionado;
import br.tcc.comparador.web.Dtos.ResultadoMetodo;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingsSimilaridadeService {

    public static final String NOME = "EMBEDDINGS";

    private final EmbeddingService embeddingService;
    private final double limiarParagrafo;

    public EmbeddingsSimilaridadeService(EmbeddingService embeddingService, AppProperties properties) {
        this.embeddingService = embeddingService;
        this.limiarParagrafo = properties.similarity().embeddingsParagraphThreshold();
    }

    public boolean ativado() {
        return embeddingService.ativado();
    }

    /** Gera os embeddings de um documento e mede quanto tempo isso levou. */
    public DocumentoEmbutido embutir(DocumentoPreparado documento) {
        long inicio = System.nanoTime();
        List<String> textos = documento.paragrafos().isEmpty()
            ? List.of(String.join(" ", documento.tokens()))
            : documento.paragrafos();
        List<float[]> vetores = embeddingService.gerar(textos);
        float[] vetorDocumento = DenseVectors.mean(vetores);
        double tempoMs = (System.nanoTime() - inicio) / 1_000_000.0;
        return new DocumentoEmbutido(vetores, vetorDocumento, tempoMs);
    }

    /** O tempo reportado inclui o custo de gerar os embeddings dos dois documentos. */
    public ResultadoMetodo comparar(DocumentoPreparado a, DocumentoEmbutido ea,
                                    DocumentoPreparado b, DocumentoEmbutido eb, Overlap overlap) {
        long inicio = System.nanoTime();

        double similaridade = DenseVectors.cosine(ea.vetorDocumento(), eb.vetorDocumento());
        List<Match> pares = ParagraphMatcher.match(
            Math.min(ea.vetoresParagrafos().size(), a.paragrafos().size()),
            Math.min(eb.vetoresParagrafos().size(), b.paragrafos().size()),
            (i, j) -> DenseVectors.cosine(ea.vetoresParagrafos().get(i), eb.vetoresParagrafos().get(j)),
            limiarParagrafo);

        List<ParagrafoRelacionado> exibidos = pares.stream()
            .limit(TfIdfSimilaridadeService.MAX_PARAGRAFOS_EXIBIDOS)
            .map(m -> new ParagrafoRelacionado(a.paragrafos().get(m.indexA()), b.paragrafos().get(m.indexB()), m.score()))
            .toList();

        double tempoMs = ea.tempoMs() + eb.tempoMs() + (System.nanoTime() - inicio) / 1_000_000.0;
        return new ResultadoMetodo(NOME, similaridade, overlap.total(), pares.size(),
            TfIdfSimilaridadeService.arredondar(tempoMs), overlap.words(), exibidos, null);
    }
}
