package br.tcc.comparador.service;

import br.tcc.comparador.core.ParagraphSplitter;
import br.tcc.comparador.core.TextPreprocessor;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PreparacaoDocumentoService {

    public DocumentoPreparado preparar(String nome, String texto) {
        List<String> paragrafos = ParagraphSplitter.split(texto);
        List<List<String>> tokensParagrafos = new ArrayList<>();
        for (String paragrafo : paragrafos) {
            tokensParagrafos.add(TextPreprocessor.tokenize(paragrafo));
        }
        List<String> tokens = TextPreprocessor.tokenize(texto);
        if (tokens.isEmpty()) {
            throw new DocumentoInvalidoException("O documento \"" + nome + "\" não tem texto suficiente para comparar.");
        }
        return new DocumentoPreparado(nome, paragrafos, tokensParagrafos, tokens);
    }
}
