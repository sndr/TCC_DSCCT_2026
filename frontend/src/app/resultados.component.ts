import { Component, ElementRef, OnDestroy, computed, effect, inject, OnInit, signal, viewChild } from '@angular/core';
import { BarController, BarElement, CategoryScale, Chart, Legend, LinearScale, Tooltip } from 'chart.js';
import { ApiService, mensagemDeErro } from './api.service';
import { HistoricoItem } from './models';
import { milissegundos, percentual } from './texto';

Chart.register(BarController, BarElement, CategoryScale, LinearScale, Tooltip, Legend);

@Component({
  selector: 'app-resultados',
  standalone: true,
  imports: [],
  template: `
    <section class="painel">
      <div class="resultados-cabecalho">
        <div>
          <h2>Histórico de resultados</h2>
          <p class="mudo">Acompanhe as similaridades e consulte as últimas comparações.</p>
        </div>
        @if (itens().length > 0) {
          <button class="botao-secundario" type="button" [disabled]="limpando()" (click)="limparHistorico()">
            {{ limpando() ? 'Limpando…' : 'Limpar histórico' }}
          </button>
        }
      </div>

      @if (carregando()) {
        <p class="mudo">Carregando histórico…</p>
      } @else if (erro()) {
        <p class="erro" role="alert">{{ erro() }}</p>
      } @else if (itens().length === 0) {
        <p class="mudo">{{ sucesso() || 'Nenhum resultado registrado ainda.' }}</p>
      } @else {
        <section class="historico-grafico" aria-label="Gráfico de similaridade do histórico">
          <div class="grafico-titulo">
            <div>
              <h3>Similaridade por comparação</h3>
              <p class="mudo">Veja lado a lado os resultados de cada método.</p>
            </div>
            <ul class="legenda-metodos" aria-label="Legenda dos métodos">
              <li><span class="legenda-ponto tfidf"></span>TF-IDF</li>
              <li><span class="legenda-ponto emb"></span>Embeddings</li>
            </ul>
          </div>
          <div class="historico-grafico-area" [style.height.px]="alturaGrafico()">
            <canvas #grafico aria-label="Similaridade percentual das comparações recentes" role="img"></canvas>
          </div>
        </section>

        <div class="tabela-rolagem">
          <table class="comparativo">
            <thead>
              <tr>
                <th>Data</th>
                <th>Par de documentos</th>
                <th>Método</th>
                <th class="num">Similaridade</th>
                <th class="num">Tempo</th>
              </tr>
            </thead>
            <tbody>
              @for (item of itens(); track item.id) {
                <tr>
                  <td>{{ item.criadoEm }}</td>
                  <td>{{ item.arquivoA }} × {{ item.arquivoB }}</td>
                  <td>{{ item.metodo }}</td>
                  <td class="num">{{ pct(item.similaridade) }}</td>
                  <td class="num">{{ ms(item.tempoMs) }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </section>
  `
})
export class ResultadosComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private chart: Chart | null = null;
  private readonly canvas = viewChild<ElementRef<HTMLCanvasElement>>('grafico');

  protected itens = signal<HistoricoItem[]>([]);
  protected carregando = signal(false);
  protected limpando = signal(false);
  protected erro = signal<string | null>(null);
  protected sucesso = signal<string | null>(null);
  protected alturaGrafico = computed(() => Math.min(680, Math.max(240, this.itens().length * 42)));

  protected pct = percentual;
  protected ms = milissegundos;

  constructor() {
    effect(() => {
      const itens = this.itens();
      const canvas = this.canvas();
      if (canvas && itens.length) {
        this.desenhar(canvas.nativeElement, itens);
      } else {
        this.chart?.destroy();
        this.chart = null;
      }
    });
  }

  ngOnInit(): void {
    this.carregarHistorico();
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  protected limparHistorico(): void {
    if (this.limpando() || !this.itens().length || !window.confirm('Deseja apagar todo o histórico de comparações?')) {
      return;
    }

    this.limpando.set(true);
    this.erro.set(null);
    this.sucesso.set(null);
    this.api.limparHistorico().subscribe({
      next: () => {
        this.itens.set([]);
        this.sucesso.set('Histórico limpo.');
        this.limpando.set(false);
      },
      error: e => {
        this.erro.set(mensagemDeErro(e));
        this.limpando.set(false);
      }
    });
  }

  private carregarHistorico(): void {
    this.carregando.set(true);
    this.erro.set(null);

    this.api.historico().subscribe({
      next: dados => {
        this.itens.set(dados);
        this.carregando.set(false);
      },
      error: e => {
        this.erro.set(mensagemDeErro(e));
        this.carregando.set(false);
      }
    });
  }

  private desenhar(canvas: HTMLCanvasElement, itens: HistoricoItem[]): void {
    this.chart?.destroy();
    this.chart = new Chart(canvas, {
      type: 'bar',
      data: {
        labels: itens.map(item => [`${item.arquivoA} × ${item.arquivoB}`, item.metodo]),
        datasets: [{
          label: 'Similaridade',
          data: itens.map(item => item.similaridade * 100),
          backgroundColor: itens.map(item => item.metodo === 'EMBEDDINGS' ? '#9b2c6e' : '#2b59c3'),
          borderRadius: 6,
          barThickness: 16
        }]
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            min: 0,
            max: 100,
            title: { display: true, text: 'Similaridade (%)' },
            ticks: { callback: valor => `${valor}%` }
          },
          y: { grid: { display: false } }
        },
        plugins: {
          legend: { display: false },
          tooltip: { callbacks: { label: contexto => ` ${Number(contexto.raw).toLocaleString('pt-BR', { maximumFractionDigits: 1 })}%` } }
        }
      }
    });
  }
}
