import { Component, inject, OnInit, signal } from '@angular/core';
import { ApiService, mensagemDeErro } from './api.service';
import { HistoricoItem } from './models';
import { milissegundos, percentual } from './texto';

@Component({
  selector: 'app-resultados',
  standalone: true,
  imports: [],
  template: `
    <section class="painel">
      <h2>Histórico de Resultados</h2>
      <p class="mudo">Registro das últimas comparações realizadas no sistema.</p>

      @if (carregando()) {
        <p class="mudo">Carregando histórico...</p>
      } @else if (erro()) {
        <p class="erro" role="alert">{{ erro() }}</p>
      } @else if (itens().length === 0) {
        <p class="mudo">Nenhum resultado registrado ainda.</p>
      } @else {
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
export class ResultadosComponent implements OnInit {
  private readonly api = inject(ApiService);

  protected itens = signal<HistoricoItem[]>([]);
  protected carregando = signal(false);
  protected erro = signal<string | null>(null);

  protected pct = percentual;
  protected ms = milissegundos;

  ngOnInit(): void {
    this.carregarHistorico();
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
}