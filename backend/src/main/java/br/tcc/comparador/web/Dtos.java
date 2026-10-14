package br.tcc.comparador.web;

import java.time.Instant;
import java.util.List;

/** Objetos trocados com o front-end Angular. */
public final class Dtos {

    /** Padrao de todo contrutor eh deixar um uma funcao vazia para evitar problemas futuros */
    private Dtos() {
    }

    public record ParagrafoRelacionado(String textoA, String textoB, double similaridade) {
    }

    public record ResultadoMetodo(
        String metodo,
        Double similaridade,
        Integer palavrasSemelhantes,
        Integer paragrafosRelacionados,
        Double tempoMs,
        List<String> palavrasEmComum,
        List<ParagrafoRelacionado> paragrafos,
        String erro) {

        public static ResultadoMetodo comErro(String metodo, String erro) {
            return new ResultadoMetodo(metodo, null, null, null, null, List.of(), List.of(), erro);
        }
    }

    public record ComparacaoResponse(
        String arquivoA,
        String arquivoB,
        int paragrafosA,
        int paragrafosB,
        int palavrasA,
        int palavrasB,
        List<ResultadoMetodo> resultados) {
    }

    public record ParBenchmark(
        int indiceA,
        int indiceB,
        Double tfidf,
        Double tfidfMs,
        Double embeddings,
        Double embeddingsMs) {
    }

    public record ResumoMetodo(String metodo, Double similaridadeMedia, Double tempoMedioMs) {
    }

    public record BenchmarkResponse(
        List<String> documentos,
        List<ParBenchmark> pares,
        List<ResumoMetodo> resumo,
        String avisoEmbeddings) {
    }

    public record HistoricoItem(
        Long id,
        String arquivoA,
        String arquivoB,
        String metodo,
        double similaridade,
        int palavrasSemelhantes,
        int paragrafosRelacionados,
        double tempoMs,
        Instant criadoEm) {
    }

    public record StatusResponse(boolean embeddingsAtivados, String modeloEmbeddings) {
    }

    public record ErroResponse(String erro) {
    }
}
