package br.tcc.comparador.service;

import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import br.tcc.comparador.config.AppProperties;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

/**
 * Gera embeddings de frases/parágrafos em Java usando Deep Java Library (DJL) com um modelo
 * sentence-transformers do HuggingFace. O modelo é baixado automaticamente na primeira chamada.
 */
@Service
public class EmbeddingService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private static final int TAMANHO_LOTE = 16;

    private final AppProperties properties;
    private ZooModel<String, float[]> modelo;

    public EmbeddingService(AppProperties properties) {
        this.properties = properties;
    }

    public boolean ativado() {
        return properties.embeddings().enabled();
    }

    public String nomeModelo() {
        return properties.embeddings().model();
    }

    public List<float[]> gerar(List<String> textos) {
        ZooModel<String, float[]> carregado = carregarModelo();
        List<float[]> vetores = new ArrayList<>(textos.size());
        try (Predictor<String, float[]> preditor = carregado.newPredictor()) {
            for (int i = 0; i < textos.size(); i += TAMANHO_LOTE) {
                List<String> lote = textos.subList(i, Math.min(i + TAMANHO_LOTE, textos.size()));
                vetores.addAll(preditor.batchPredict(lote));
            }
        } catch (Exception | LinkageError e) {
            log.error("Falha ao gerar embeddings", e);
            throw new EmbeddingIndisponivelException("Falha ao gerar embeddings: " + descrever(e), e);
        }
        return vetores;
    }

    private synchronized ZooModel<String, float[]> carregarModelo() {
        if (!ativado()) {
            throw new EmbeddingIndisponivelException("Embeddings estão desativados (app.embeddings.enabled=false).");
        }
        if (modelo != null) {
            return modelo;
        }
        log.info("Carregando modelo de embeddings {} (o download ocorre na primeira execução)...", nomeModelo());
        try {
            Criteria<String, float[]> criteria = Criteria.builder()
                    .setTypes(String.class, float[].class)
                    .optModelUrls("djl://ai.djl.huggingface.pytorch/" + nomeModelo())
                    .optEngine("PyTorch")
                    .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                    .optProgress(new ProgressBar())
                    .build();
            modelo = criteria.loadModel();
            log.info("Modelo de embeddings carregado.");
            return modelo;
        } catch (Exception | LinkageError e) {
            log.error("Falha ao carregar o modelo de embeddings", e);
            throw new EmbeddingIndisponivelException(
                    "Não foi possível carregar o modelo de embeddings (" + nomeModelo()
                            + "). Verifique a conexão com a internet no primeiro uso. Detalhe: " + descrever(e), e);
        }
    }

    /** Mensagem legível: tipo do erro + causa raiz (muitas exceções do DJL vêm com message nula). */
    private static String descrever(Throwable erro) {
        Throwable raiz = erro;
        while (raiz.getCause() != null && raiz.getCause() != raiz) {
            raiz = raiz.getCause();
        }
        String base = erro.getClass().getSimpleName() + (erro.getMessage() == null ? "" : ": " + erro.getMessage());
        if (raiz != erro) {
            base += " (causa: " + raiz.getClass().getSimpleName()
                    + (raiz.getMessage() == null ? "" : ": " + raiz.getMessage()) + ")";
        }
        return base;
    }

    @Override
    public synchronized void destroy() {
        if (modelo != null) {
            modelo.close();
        }
    }
}