package br.tcc.comparador.core;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Pré-processamento de texto: minúsculas, remoção de acentos, tokenização
 * e remoção de stopwords (português e inglês).
 */
public final class TextPreprocessor {

    private static final Set<String> STOPWORDS = Set.of(
        // português
        "as", "os", "uma", "uns", "umas", "dos", "das", "nos", "nas", "por", "para", "pra", "com", "sem",
        "sob", "sobre", "entre", "ate", "apos", "ante", "mas", "nem", "que", "como", "quando", "onde",
        "porque", "pois", "porem", "contudo", "aos", "pelo", "pela", "pelos", "pelas", "num", "numa",
        "dum", "duma", "ser", "sao", "foi", "era", "sera", "estar", "esta", "estao", "estava", "ter",
        "tem", "tinha", "haver", "mais", "menos", "muito", "muita", "muitos", "muitas", "pouco",
        "tambem", "ainda", "entao", "isso", "isto", "aquilo", "esse", "essa", "esses", "essas", "este",
        "estes", "estas", "aquele", "aquela", "aqueles", "aquelas", "seu", "sua", "seus", "suas", "meu",
        "minha", "nosso", "nossa", "ele", "ela", "eles", "elas", "voce", "voces", "lhe", "lhes", "cada",
        "qual", "quais", "quem", "outro", "outra", "outros", "outras", "mesmo", "mesma", "todo", "toda",
        "todos", "todas", "pode", "podem", "deve", "devem", "fazer", "feito", "sendo", "sido", "nao",
        "sim", "vez", "vezes", "assim", "dessa", "desse", "nesta", "neste", "dessas", "desses",
        // inglês
        "the", "and", "but", "for", "with", "from", "are", "was", "were", "been", "being", "its", "this",
        "that", "these", "those", "not", "yes", "then", "than", "such", "can", "could", "will", "would",
        "should", "may", "might", "have", "has", "had", "does", "did", "she", "they", "you", "his",
        "her", "their", "our", "your", "which", "who", "whom", "what", "when", "where", "why", "how",
        "also", "into", "about", "there", "here", "all", "any", "each", "more", "most", "other"
    );

    private static final int MIN_TOKEN_LENGTH = 3;

    private TextPreprocessor() {
    }

    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase();
    }

    public static List<String> tokenize(String text) {
        String normalized = normalize(text);
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (Character.isLetter(c)) {
                current.append(c);
            } else {
                flush(current, tokens);
            }
        }
        flush(current, tokens);
        return tokens;
    }

    private static void flush(StringBuilder current, List<String> tokens) {
        if (current.length() >= MIN_TOKEN_LENGTH) {
            String token = current.toString();
            if (!STOPWORDS.contains(token)) {
                tokens.add(token);
            }
        }
        current.setLength(0);
    }
}
