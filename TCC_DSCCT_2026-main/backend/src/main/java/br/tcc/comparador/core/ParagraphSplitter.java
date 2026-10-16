package br.tcc.comparador.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Divide um texto em parágrafos. Se o texto vier "colado" (comum em PDFs),
 * cai para uma divisão por grupos de sentenças.
 */
public final class ParagraphSplitter {

    private static final int MIN_PARAGRAPH_CHARS = 25;
    private static final int MAX_PARAGRAPH_CHARS = 1200;
    private static final int SENTENCES_PER_CHUNK = 3;
    public static final int MAX_PARAGRAPHS = 300;

    private ParagraphSplitter() {
    }

    public static List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String unified = text.replace("\r\n", "\n").replace('\r', '\n');
        List<String> result = new ArrayList<>();

        for (String block : unified.split("\\n\\s*\\n")) {
            String clean = block.replaceAll("\\s+", " ").trim();
            if (clean.length() < MIN_PARAGRAPH_CHARS) {
                continue;
            }
            if (clean.length() > MAX_PARAGRAPH_CHARS) {
                result.addAll(chunkBySentences(clean));
            } else {
                result.add(clean);
            }
        }

        if (result.size() < 3) {
            String flat = unified.replaceAll("\\s+", " ").trim();
            List<String> chunks = chunkBySentences(flat);
            if (chunks.size() > result.size()) {
                result = chunks;
            }
        }

        if (result.size() > MAX_PARAGRAPHS) {
            return new ArrayList<>(result.subList(0, MAX_PARAGRAPHS));
        }
        return result;
    }

    private static List<String> chunkBySentences(String text) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder current = new StringBuilder();
        int count = 0;
        for (String sentence : sentences) {
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(sentence);
            count++;
            if (count >= SENTENCES_PER_CHUNK || current.length() > MAX_PARAGRAPH_CHARS) {
                addIfLongEnough(chunks, current);
                current.setLength(0);
                count = 0;
            }
        }
        addIfLongEnough(chunks, current);
        return chunks;
    }

    private static void addIfLongEnough(List<String> chunks, StringBuilder current) {
        String value = current.toString().trim();
        if (value.length() >= MIN_PARAGRAPH_CHARS) {
            chunks.add(value);
        }
    }
}
