package br.tcc.comparador.service;

import java.util.List;

/** Embeddings dos parágrafos e do documento (média dos parágrafos). */
public record DocumentoEmbutido(List<float[]> vetoresParagrafos, float[] vetorDocumento, double tempoMs) {
}
