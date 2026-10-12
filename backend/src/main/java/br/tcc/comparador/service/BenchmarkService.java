package br.tcc.comparador.service;

import br.tcc.comparador.core.LexicalOverlap;
import br.tcc.comparador.core.LexicalOverlap.Overlap;
import br.tcc.comparador.web.Dtos.BenchmarkResponse;
import br.tcc.comparador.web.Dtos.ParBenchmark;
import br.tcc.comparador.web.Dtos.ResultadoMetodo;
import br.tcc.comparador.web.Dtos.ResumoMetodo;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Compara todos os pares de uma coleção de documentos com TF-IDF e embeddings. */
@Service
public class BenchmarkService {

    public static final int MIN_DOCUMENTOS = 2;
    public static final int MAX_DOCUMENTOS = 6;

    private final ExtracaoTextoService extracao;
    private final PreparacaoDocumentoService preparacao;
    private final TfIdfSimilaridadeService tfIdf;
    private final EmbeddingsSimilaridadeService embeddings;

    public BenchmarkService(ExtracaoTextoService extracao,
                            PreparacaoDocumentoService preparacao,
                            TfIdfSimilaridadeService tfIdf,
                            EmbeddingsSimilaridadeService embeddings) {
        this.extracao = extracao;
        this.preparacao = preparacao;
        this.tfIdf = tfIdf;
        this.embeddings = embeddings;
    }

    public BenchmarkResponse executar(List<MultipartFile> arquivos) {
        if (arquivos == null || arquivos.size() < MIN_DOCUMENTOS || arquivos.size() > MAX_DOCUMENTOS) {
            throw new DocumentoInvalidoException(
                "Envie de " + MIN_DOCUMENTOS + " a " + MAX_DOCUMENTOS + " documentos para comparar os algoritmos.");
        }

        List<DocumentoPreparado> documentos = new ArrayList<>();
        for (MultipartFile arquivo : arquivos) {
            documentos.add(preparacao.preparar(ExtracaoTextoService.nomeDe(arquivo), extracao.extrair(arquivo)));
        }

        List<DocumentoEmbutido> embutidos = new ArrayList<>();
        String aviso = null;
        try {
            for (DocumentoPreparado documento : documentos) {
                embutidos.add(embeddings.embutir(documento));
            }
        } catch (EmbeddingIndisponivelException e) {
            embutidos.clear();
            aviso = e.getMessage();
        }
        boolean comEmbeddings = !embutidos.isEmpty();

        List<ParBenchmark> pares = new ArrayList<>();
        for (int i = 0; i < documentos.size(); i++) {
            for (int j = i + 1; j < documentos.size(); j++) {
                DocumentoPreparado a = documentos.get(i);
                DocumentoPreparado b = documentos.get(j);
                Overlap overlap = LexicalOverlap.shared(a.tokens(), b.tokens(), 1);

                ResultadoMetodo rTfIdf = tfIdf.comparar(a, b, overlap);
                ResultadoMetodo rEmb = comEmbeddings
                    ? embeddings.comparar(a, embutidos.get(i), b, embutidos.get(j), overlap)
                    : null;

                pares.add(new ParBenchmark(i, j,
                    rTfIdf.similaridade(), rTfIdf.tempoMs(),
                    rEmb == null ? null : rEmb.similaridade(),
                    rEmb == null ? null : rEmb.tempoMs()));
            }
        }

        List<ResumoMetodo> resumo = new ArrayList<>();
        resumo.add(new ResumoMetodo(TfIdfSimilaridadeService.NOME,
            media(pares.stream().map(ParBenchmark::tfidf).toList()),
            media(pares.stream().map(ParBenchmark::tfidfMs).toList())));
        if (comEmbeddings) {
            resumo.add(new ResumoMetodo(EmbeddingsSimilaridadeService.NOME,
                media(pares.stream().map(ParBenchmark::embeddings).toList()),
                media(pares.stream().map(ParBenchmark::embeddingsMs).toList())));
        }

        return new BenchmarkResponse(documentos.stream().map(DocumentoPreparado::nome).toList(), pares, resumo, aviso);
    }

    private static Double media(List<Double> valores) {
        return valores.stream().mapToDouble(Double::doubleValue).average()
            .stream().boxed().map(v -> Math.round(v * 10000.0) / 10000.0).findFirst().orElse(null);
    }
}
