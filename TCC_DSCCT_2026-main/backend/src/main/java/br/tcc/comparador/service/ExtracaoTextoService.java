package br.tcc.comparador.service;

import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Extrai texto de PDF, DOCX, TXT e outros formatos usando Apache Tika. */
@Service
public class ExtracaoTextoService {

    private static final int LIMITE_CARACTERES = 5_000_000;

    private final Tika tika = new Tika();

    public ExtracaoTextoService() {
        tika.setMaxStringLength(LIMITE_CARACTERES);
    }

    public String extrair(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new DocumentoInvalidoException("O arquivo enviado está vazio.");
        }
        String nome = nomeDe(arquivo);
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, nome);
        try (InputStream entrada = arquivo.getInputStream()) {
            String texto = tika.parseToString(entrada, metadata);
            if (texto == null || texto.isBlank()) {
                throw new DocumentoInvalidoException(
                    "Não foi possível extrair texto de \"" + nome + "\". Se for um PDF escaneado, ele precisa de OCR antes.");
            }
            return texto;
        } catch (IOException | TikaException e) {
            throw new DocumentoInvalidoException("Não foi possível ler \"" + nome + "\": " + e.getMessage(), e);
        }
    }

    public static String nomeDe(MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename();
        return (nome == null || nome.isBlank()) ? "documento" : nome;
    }
}
