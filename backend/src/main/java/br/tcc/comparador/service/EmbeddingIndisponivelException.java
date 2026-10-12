package br.tcc.comparador.service;

public class EmbeddingIndisponivelException extends RuntimeException {

    public EmbeddingIndisponivelException(String message) {
        super(message);
    }

    public EmbeddingIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
