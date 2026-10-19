import { Component, signal } from '@angular/core';
import { BenchmarkComponent } from './benchmark.component';
import { CompararComponent } from './comparar.component';
import { ResultadosComponent } from './resultados.component'; // 1. Importe aqui

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CompararComponent, BenchmarkComponent, ResultadosComponent], // 2. Adicione nos imports
  template: `
    <header class="topo">
      <h1>Comparador de documentos</h1>
      <nav aria-label="Telas">
        <button type="button" [class.ativa]="tela() === 'comparar'" (click)="tela.set('comparar')">Comparar dois documentos</button>
        <button type="button" [class.ativa]="tela() === 'algoritmos'" (click)="tela.set('algoritmos')">Comparação dos algoritmos</button>
        <button type="button" [class.ativa]="tela() === 'resultados'" (click)="tela.set('resultados')">Resultados</button>
      </nav>
    </header>
    <main>
      @if (tela() === 'comparar') {
        <app-comparar />
      } @else if (tela() === 'algoritmos') {
        <app-benchmark />
      } @else if (tela() === 'resultados') {
        <app-resultados /> }
    </main>
  `
})
export class AppComponent {
  protected tela = signal<'comparar' | 'algoritmos' | 'resultados'>('comparar');
}