import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ApiService, mensagemDeErro } from './api.service';
import { DropzoneComponent } from './dropzone.component';
import { ComparacaoResponse, HistoricoItem, Metodo, ResultadoMetodo, Status } from './models';
import { Trecho, destacar, milissegundos, nomeDoMetodo, normalizar, percentual } from './texto';

interface ParagrafoVM {
  trechosA: Trecho[];
  trechosB: Trecho[];
  similaridade: number;
}

interface ResultadoVM {
  resultado: ResultadoMetodo;
  classe: 'tfidf' | 'emb';
  paragrafos: ParagrafoVM[];
  palavras: string[];
  jaccard: number | null;
}

@Component({
  selector: 'app-comparar',
  standalone: true,
  imports: [DropzoneComponent, DatePipe],
  template: `
    <section class="painel">
      <section class="intro-comparacao" aria-labelledby="intro-comparacao-titulo">
        <div class="intro-cabecalho">
          <span class="intro-selo">COMPARADOR DE DOCUMENTOS</span>
          <h2 id="intro-comparacao-titulo">Compare palavras e significados</h2>
          <p class="intro-lead">Envie dois documentos para analisar a similaridade, encontrar termos em comum e localizar parágrafos relacionados.</p>
        </div>
        <div class="intro-metodos">
          <article class="intro-metodo tfidf">
            <h3>TF-IDF</h3>
            <p>Pondera os termos pela frequência e pelo quanto ajudam a distinguir o conteúdo de cada documento.</p>
          </article>
          <article class="intro-metodo emb">
            <h3>Embeddings</h3>
            <p>Transforma trechos em vetores numéricos que representam contexto e significado. A proximidade entre esses vetores ajuda a encontrar conteúdos parecidos mesmo com palavras diferentes.</p>
          </article>
        </div>
        <p class="intro-formatos">Aceita arquivos PDF, DOCX, TXT, ODT e RTF. Escolha um método ou compare os dois lado a lado.</p>
      </section>

      <div class="duplo">
        <app-dropzone titulo="Documento A" [arquivoAtual]="arquivoA()" (escolhidos)="arquivoA.set($event[0])" />
        <app-dropzone titulo="Documento B" [arquivoAtual]="arquivoB()" (escolhidos)="arquivoB.set($event[0])" />
      </div>

      <div class="controles">
        <fieldset class="metodos">
          <legend>Método de comparação</legend>
          @for (opcao of opcoes; track opcao.valor) {
            <label class="opcao" [class.marcada]="metodo() === opcao.valor"
                   [class.desabilitada]="opcao.valor !== 'TFIDF' && status() && !status()!.embeddingsAtivados">
              <input type="radio" name="metodo" [value]="opcao.valor"
                     [checked]="metodo() === opcao.valor" (change)="metodo.set(opcao.valor)">
              {{ opcao.rotulo }}
            </label>
          }
        </fieldset>
        <button class="botao" type="button" [disabled]="!podeComparar()" (click)="comparar()">
          {{ carregando() ? 'Comparando…' : 'Comparar' }}
        </button>
      </div>

      @if (carregando() && metodo() !== 'TFIDF') {
        <p class="aviso">Na primeira execução com embeddings, o modelo é baixado (cerca de 470 MB). Isso pode levar alguns minutos.</p>
      }
      @if (erro()) {
        <p class="erro" role="alert">{{ erro() }}</p>
      }
    </section>

    @if (resposta(); as r) {
      <section class="resultado">
        <p class="resumo-docs">
          <strong>{{ r.arquivoA }}</strong> × <strong>{{ r.arquivoB }}</strong>
          <span class="mudo">{{ r.paragrafosA }} e {{ r.paragrafosB }} parágrafos · {{ r.palavrasA }} e {{ r.palavrasB }} termos úteis</span>
        </p>

        <div class="cartoes" [class.dois]="visoes().length > 1">
          @for (v of visoes(); track v.resultado.metodo) {
            <article class="cartao" [class]="'cartao ' + v.classe">
              <h2>{{ nome(v.resultado.metodo) }}</h2>
              @if (v.resultado.erro) {
                <p class="erro">{{ v.resultado.erro }}</p>
              } @else {
                <p class="numero">{{ pct(v.resultado.similaridade) }}</p>
                <div class="barra" role="img" [attr.aria-label]="'Similaridade ' + pct(v.resultado.similaridade)">
                  <div class="barra-preenchida" [style.width.%]="(v.resultado.similaridade ?? 0) * 100"></div>
                </div>
                <dl class="metricas">
                  <div><dt>Palavras em comum</dt><dd>{{ v.resultado.palavrasSemelhantes }}</dd></div>
                  <div><dt>Percentual de palavras em comum (Jaccard)</dt><dd class="jaccard-valor">{{ pct(v.jaccard) }}</dd></div>
                  <div><dt>Parágrafos relacionados</dt><dd>{{ v.resultado.paragrafosRelacionados }}</dd></div>
                  <div><dt>Tempo</dt><dd>{{ ms(v.resultado.tempoMs) }}</dd></div>
                </dl>
              }
            </article>
          }
        </div>

        @for (v of visoes(); track v.resultado.metodo) {
          @if (!v.resultado.erro) {
            <section class="palavras-painel" [class]="'palavras-painel ' + v.classe" aria-label="Palavras em comum">
              <div class="palavras-cabecalho">
                <div>
                  <h2>Palavras em comum</h2>
                  <p class="mudo">Termos encontrados nos dois documentos com {{ nome(v.resultado.metodo) }}.</p>
                </div>
                <span class="palavras-contagem">{{ v.resultado.palavrasSemelhantes ?? 0 }} termos</span>
              </div>
              @if (v.palavras.length) {
                <ul class="chips">
                  @for (palavra of v.palavras; track palavra) {
                    <li>{{ palavra }}</li>
                  }
                </ul>
                @if ((v.resultado.palavrasEmComum.length) > v.palavras.length) {
                  <p class="mudo">Exibindo os primeiros {{ v.palavras.length }} termos.</p>
                }
              } @else {
                <p class="mudo">Nenhuma palavra em comum foi identificada.</p>
              }
            </section>
          }
        }

        @for (v of visoes(); track v.resultado.metodo) {
          @if (!v.resultado.erro) {
            <details class="pares" [class]="'pares ' + v.classe" [open]="visoes().length === 1">
              <summary>Parágrafos relacionados ({{ nome(v.resultado.metodo) }}) — {{ v.paragrafos.length }} de {{ v.resultado.paragrafosRelacionados }}</summary>
              @if (!v.paragrafos.length) {
                <p class="mudo">Nenhum par de parágrafos passou do limiar de similaridade.</p>
              }
              @for (par of v.paragrafos; track $index) {
                <div class="par">
                  <p class="par-score">{{ pct(par.similaridade) }}</p>
                  <div class="par-documento">
                    <span class="par-rotulo">Documento A</span>
                    <p class="par-texto">
                      @for (t of par.trechosA; track $index) {<span [class.destaque]="t.destaque">{{ t.texto }}</span>}
                    </p>
                  </div>
                  <div class="par-documento">
                    <span class="par-rotulo">Documento B</span>
                    <p class="par-texto">
                      @for (t of par.trechosB; track $index) {<span [class.destaque]="t.destaque">{{ t.texto }}</span>}
                    </p>
                  </div>
                </div>
              }
            </details>
          }
        }
      </section>
    }

    <section class="historico">
      <div class="resultados-cabecalho">
        <h2>Comparações anteriores</h2>
        @if (historico().length > 0) {
          <button class="botao-secundario" type="button" [disabled]="limpandoHistorico()" (click)="limparHistorico()">
            {{ limpandoHistorico() ? 'Limpando…' : 'Limpar histórico' }}
          </button>
        }
      </div>
      @if (!historico().length) {
        <p class="mudo">Nada salvo ainda. Cada comparação feita aparece aqui.</p>
      } @else {
        <div class="tabela-rolagem">
          <table>
            <thead>
              <tr><th>Data</th><th>Documentos</th><th>Método</th><th class="num">Similaridade</th><th class="num">Tempo</th></tr>
            </thead>
            <tbody>
              @for (h of historico(); track h.id) {
                <tr>
                  <td>{{ h.criadoEm | date: 'dd/MM HH:mm' }}</td>
                  <td>{{ h.arquivoA }} × {{ h.arquivoB }}</td>
                  <td>{{ nome(h.metodo) }}</td>
                  <td class="num">{{ pct(h.similaridade) }}</td>
                  <td class="num">{{ ms(h.tempoMs) }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `
})
export class CompararComponent implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly opcoes: { valor: Metodo; rotulo: string }[] = [
    { valor: 'TFIDF', rotulo: 'TF-IDF' },
    { valor: 'EMBEDDINGS', rotulo: 'Embeddings' },
    { valor: 'AMBOS', rotulo: 'Os dois' }
  ];

  protected arquivoA = signal<File | null>(null);
  protected arquivoB = signal<File | null>(null);
  protected metodo = signal<Metodo>('TFIDF');
  protected carregando = signal(false);
  protected erro = signal<string | null>(null);
  protected resposta = signal<ComparacaoResponse | null>(null);
  protected historico = signal<HistoricoItem[]>([]);
  protected limpandoHistorico = signal(false);
  protected status = signal<Status | null>(null);

  protected podeComparar = computed(() => !!this.arquivoA() && !!this.arquivoB() && !this.carregando());

  protected visoes = computed<ResultadoVM[]>(() => {
    const r = this.resposta();
    if (!r) {
      return [];
    }
    return r.resultados.map(resultado => {
      const conjunto = new Set(resultado.palavrasEmComum.map(normalizar));
      const intersecao = resultado.palavrasSemelhantes ?? 0;
      const uniao = r.palavrasUnicasA + r.palavrasUnicasB - intersecao;
      const jaccard = uniao > 0 ? intersecao / uniao : null;

      return {
        resultado,
        classe: resultado.metodo === 'EMBEDDINGS' ? 'emb' : 'tfidf',
        palavras: resultado.palavrasEmComum.slice(0, 30),
        jaccard,
        paragrafos: resultado.paragrafos.map(p => ({
          trechosA: destacar(p.textoA, conjunto),
          trechosB: destacar(p.textoB, conjunto),
          similaridade: p.similaridade
        }))
      };
    });
  });

  protected pct = percentual;
  protected ms = milissegundos;
  protected nome = nomeDoMetodo;

  ngOnInit(): void {
    this.api.status().subscribe({ next: s => this.status.set(s), error: () => undefined });
    this.carregarHistorico();
  }

  protected comparar(): void {
    const a = this.arquivoA();
    const b = this.arquivoB();
    if (!a || !b) {
      return;
    }
    this.carregando.set(true);
    this.erro.set(null);
    this.api.comparar(a, b, this.metodo()).subscribe({
      next: resposta => {
        this.resposta.set(resposta);
        this.carregando.set(false);
        this.carregarHistorico();
      },
      error: e => {
        this.erro.set(mensagemDeErro(e));
        this.carregando.set(false);
      }
    });
  }

  protected limparHistorico(): void {
    if (this.limpandoHistorico() || !this.historico().length || !window.confirm('Deseja apagar todo o histórico de comparações?')) {
      return;
    }
    this.limpandoHistorico.set(true);
    this.erro.set(null);
    this.api.limparHistorico().subscribe({
      next: () => {
        this.historico.set([]);
        this.limpandoHistorico.set(false);
      },
      error: e => {
        this.erro.set(mensagemDeErro(e));
        this.limpandoHistorico.set(false);
      }
    });
  }

  private carregarHistorico(): void {
    this.api.historico().subscribe({ next: h => this.historico.set(h), error: () => undefined });
  }
}
