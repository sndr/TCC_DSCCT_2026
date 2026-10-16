package br.tcc.comparador.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "comparacoes")
public class Comparacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String arquivoA;

    @Column(nullable = false)
    private String arquivoB;

    @Column(nullable = false, length = 20)
    private String metodo;

    @Column(nullable = false)
    private double similaridade;

    @Column(nullable = false)
    private int palavrasSemelhantes;

    @Column(nullable = false)
    private int paragrafosRelacionados;

    @Column(nullable = false)
    private double tempoMs;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();

    protected Comparacao() {
    }

    public Comparacao(String arquivoA, String arquivoB, String metodo, double similaridade,
                      int palavrasSemelhantes, int paragrafosRelacionados, double tempoMs) {
        this.arquivoA = arquivoA;
        this.arquivoB = arquivoB;
        this.metodo = metodo;
        this.similaridade = similaridade;
        this.palavrasSemelhantes = palavrasSemelhantes;
        this.paragrafosRelacionados = paragrafosRelacionados;
        this.tempoMs = tempoMs;
    }

    public Long getId() { return id; }
    public String getArquivoA() { return arquivoA; }
    public String getArquivoB() { return arquivoB; }
    public String getMetodo() { return metodo; }
    public double getSimilaridade() { return similaridade; }
    public int getPalavrasSemelhantes() { return palavrasSemelhantes; }
    public int getParagrafosRelacionados() { return paragrafosRelacionados; }
    public double getTempoMs() { return tempoMs; }
    public Instant getCriadoEm() { return criadoEm; }
}
