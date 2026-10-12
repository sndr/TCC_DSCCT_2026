package br.tcc.comparador.service;

import br.tcc.comparador.core.LexicalOverlap;
import br.tcc.comparador.core.LexicalOverlap.Overlap;
import br.tcc.comparador.domain.Comparacao;
import br.tcc.comparador.domain.ComparacaoRepository;
import br.tcc.comparador.domain.Metodo;
import br.tcc.comparador.web.Dtos.ComparacaoResponse;
import br.tcc.comparador.web.Dtos.HistoricoItem;
import br.tcc.comparador.web.Dtos.ResultadoMetodo;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ComparacaoService {

    private static final int LIMITE_PALAVRAS_EM_COMUM = 400;

    private final ExtracaoTextoService extracao;
    private final PreparacaoDocumentoService preparacao;
    private final TfIdfSimilaridadeService tfIdf;
    private final EmbeddingsSimilaridadeService embeddings;
    private final ComparacaoRepository repositorio;

    public ComparacaoService(ExtracaoTextoService extracao,
                             PreparacaoDocumentoService preparacao,
                             TfIdfSimilaridadeService tfIdf,
                             EmbeddingsSimilaridadeService embeddings,
                             ComparacaoRepository repositorio) {
        this.extracao = extracao;
        this.preparacao = preparacao;
        this.tfIdf = tfIdf;
        this.embeddings = embeddings;
        this.repositorio = repositorio;
    }

    public ComparacaoResponse comparar(MultipartFile arquivoA, MultipartFile arquivoB, Metodo metodo) {
        DocumentoPreparado a = preparar(arquivoA);
        DocumentoPreparado b = preparar(arquivoB);
        Overlap overlap = LexicalOverlap.shared(a.tokens(), b.tokens(), LIMITE_PALAVRAS_EM_COMUM);

        List<ResultadoMetodo> resultados = new ArrayList<>();
        if (metodo == Metodo.TFIDF || metodo == Metodo.AMBOS) {
            resultados.add(tfIdf.comparar(a, b, overlap));
        }
        if (metodo == Metodo.EMBEDDINGS || metodo == Metodo.AMBOS) {
            resultados.add(compararEmbeddings(a, b, overlap));
        }

        for (ResultadoMetodo resultado : resultados) {
            if (resultado.erro() == null) {
                repositorio.save(new Comparacao(a.nome(), b.nome(), resultado.metodo(),
                    resultado.similaridade(), resultado.palavrasSemelhantes(),
                    resultado.paragrafosRelacionados(), resultado.tempoMs()));
            }
        }

        return new ComparacaoResponse(a.nome(), b.nome(), a.paragrafos().size(), b.paragrafos().size(),
            a.tokens().size(), b.tokens().size(), resultados);
    }

    public List<HistoricoItem> historico() {
        return repositorio.findTop30ByOrderByCriadoEmDesc().stream()
            .map(c -> new HistoricoItem(c.getId(), c.getArquivoA(), c.getArquivoB(), c.getMetodo(),
                c.getSimilaridade(), c.getPalavrasSemelhantes(), c.getParagrafosRelacionados(),
                c.getTempoMs(), c.getCriadoEm()))
            .toList();
    }

    private ResultadoMetodo compararEmbeddings(DocumentoPreparado a, DocumentoPreparado b, Overlap overlap) {
        try {
            DocumentoEmbutido ea = embeddings.embutir(a);
            DocumentoEmbutido eb = embeddings.embutir(b);
            return embeddings.comparar(a, ea, b, eb, overlap);
        } catch (EmbeddingIndisponivelException e) {
            return ResultadoMetodo.comErro(EmbeddingsSimilaridadeService.NOME, e.getMessage());
        }
    }

    private DocumentoPreparado preparar(MultipartFile arquivo) {
        String texto = extracao.extrair(arquivo);
        return preparacao.preparar(ExtracaoTextoService.nomeDe(arquivo), texto);
    }
}
