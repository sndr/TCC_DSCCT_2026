import { Component, ElementRef, computed, effect, inject, signal, viewChild } from '@angular/core';
import { Chart, BarController, BarElement, CategoryScale, Legend, LinearScale, Tooltip } from 'chart.js';
import { ApiService, mensagemDeErro } from './api.service';
import { DropzoneComponent } from './dropzone.component';
import { BenchmarkResponse } from './models';
import { milissegundos, nomeDoMetodo, percentual } from './texto';

Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip, Legend);

const MAX_DOCUMENTOS = 6;

@Component({
  selector: 'app-benchmark',
  standalone: true,
  imports: [DropzoneComponent],
  template: `
    <section class="painel">
      <app-dropzone titulo="Coleção de documentos" [multiplo]="true"
                    (escolhidos)="adicionar($event)" />

      @if (arquivos().length) {
        <ol class="lista-docs">
          @for (arquivo of arquivos(); track $index) {
            <li>
              <span class="rotulo-doc">Documento {{ $index + 1 }}</span>
              <span class="nome-doc">{{ arquivo.name }}</span>
              <button type="button" class="link" (click)="remover($index)">Remover</button>
            </li>
          }
        </ol>
      }

      <div class="controles">
        <p class="mudo">Compara todos os pares com TF-IDF e embeddings. Envie de 2 a {{ max }} documentos.</p>
        <button class="botao" type="button" [disabled]="!podeExecutar()" (click)="executar()">
          {{ carregando() ? 'Comparando…' : 'Comparar algoritmos' }}
        </button>
      </div>

      @if (carregando()) {
        <p class="aviso">Gerar embeddings leva mais tempo que TF-IDF. Na primeira execução o modelo também é baixado.</p>
      }
      @if (erro()) {
        <p class="erro" role="alert">{{ erro() }}</p>
      }
    </section>

    @if (resposta(); as r) {
      <section class="resultado">
        @if (r.avisoEmbeddings) {
          <p class="erro">Embeddings indisponíveis: {{ r.avisoEmbeddings }}</p>
        }

        <div class="tabela-rolagem">
          <table class="comparativo">
            <thead>
              <tr>
                <th>Par de documentos</th>
                <th class="num tfidf-cab">TF-IDF</th>
                @if (temEmbeddings()) { <th class="num emb-cab">Embeddings</th> }
              </tr>
            </thead>
            <tbody>
              @for (p of r.pares; track $index) {
                <tr>
                  <td>Documento {{ p.indiceA + 1 }} × {{ p.indiceB + 1 }}</td>
                  <td class="num">{{ pct(p.tfidf) }}</td>
                  @if (temEmbeddings()) { <td class="num">{{ pct(p.embeddings) }}</td> }
                </tr>
              }
            </tbody>
            <tfoot>
              <tr>
                <td>Similaridade média</td>
                @for (m of r.resumo; track m.metodo) { <td class="num">{{ pct(m.similaridadeMedia) }}</td> }
              </tr>
              <tr>
                <td>Tempo médio por par</td>
                @for (m of r.resumo; track m.metodo) { <td class="num">{{ ms(m.tempoMedioMs) }}</td> }
              </tr>
            </tfoot>
          </table>
        </div>

        <div class="grafico">
          <canvas #grafico></canvas>
        </div>

        <ul class="legenda-docs">
          @for (nome of r.documentos; track $index) {
            <li><strong>Documento {{ $index + 1 }}</strong> {{ nome }}</li>
          }
        </ul>
      </section>
    }
  `
})
export class BenchmarkComponent {
  private readonly api = inject(ApiService);
  private chart: Chart | null = null;
  private readonly canvas = viewChild<ElementRef<HTMLCanvasElement>>('grafico');

  protected readonly max = MAX_DOCUMENTOS;
  protected arquivos = signal<File[]>([]);
  protected carregando = signal(false);
  protected erro = signal<string | null>(null);
  protected resposta = signal<BenchmarkResponse | null>(null);

  protected podeExecutar = computed(() => this.arquivos().length >= 2 && !this.carregando());
  protected temEmbeddings = computed(() => (this.resposta()?.resumo ?? []).some(m => m.metodo === 'EMBEDDINGS'));

  protected pct = percentual;
  protected ms = milissegundos;
  protected nome = nomeDoMetodo;

  constructor() {
    effect(() => {
      const resposta = this.resposta();
      const elemento = this.canvas();
      if (resposta && elemento) {
        this.desenhar(elemento.nativeElement, resposta);
      }
    });
  }

  protected adicionar(novos: File[]): void {
    this.arquivos.update(atuais => [...atuais, ...novos].slice(0, MAX_DOCUMENTOS));
  }

  protected remover(indice: number): void {
    this.arquivos.update(atuais => atuais.filter((_, i) => i !== indice));
  }

  protected executar(): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.api.benchmark(this.arquivos()).subscribe({
      next: resposta => {
        this.resposta.set(resposta);
        this.carregando.set(false);
      },
      error: e => {
        this.erro.set(mensagemDeErro(e));
        this.carregando.set(false);
      }
    });
  }

  private desenhar(canvas: HTMLCanvasElement, r: BenchmarkResponse): void {
    this.chart?.destroy();
    const rotulos = r.pares.map(p => `${p.indiceA + 1} × ${p.indiceB + 1}`);
    const conjuntos = [
      { label: 'TF-IDF', data: r.pares.map(p => (p.tfidf ?? 0) * 100), backgroundColor: '#2B59C3' }
    ];
    if (this.temEmbeddings()) {
      conjuntos.push({ label: 'Embeddings', data: r.pares.map(p => (p.embeddings ?? 0) * 100), backgroundColor: '#9B2C6E' });
    }
    this.chart = new Chart(canvas, {
      type: 'bar',
      data: { labels: rotulos, datasets: conjuntos },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: { min: 0, max: 100, title: { display: true, text: 'Similaridade (%)' } },
          x: { title: { display: true, text: 'Par de documentos' } }
        },
        plugins: { legend: { position: 'bottom' } }
      }
    });
  }
}
