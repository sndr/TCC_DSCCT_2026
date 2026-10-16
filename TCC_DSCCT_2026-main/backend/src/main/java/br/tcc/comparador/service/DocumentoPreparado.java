package br.tcc.comparador.service;

import java.util.List;

/** Documento já extraído e pré-processado, pronto para qualquer algoritmo. */
public record DocumentoPreparado(
    String nome,
    List<String> paragrafos,
    List<List<String>> tokensParagrafos,
    List<String> tokens) {
}
