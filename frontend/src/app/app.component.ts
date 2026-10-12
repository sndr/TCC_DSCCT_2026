import { Component, signal } from '@angular/core';
import { BenchmarkComponent } from './benchmark.component';
import { CompararComponent } from './comparar.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CompararComponent, BenchmarkComponent],
  template: `
    <header class="topo">
      <h1>Comparador de documentos</h1>
      <nav aria-label="Telas">
        <button type="button" [class.ativa]="tela() === 'comparar'" (click)="tela.set('comparar')">Comparar dois documentos</button>
        <button type="button" [class.ativa]="tela() === 'algoritmos'" (click)="tela.set('algoritmos')">Comparação dos algoritmos</button>
      </nav>
    </header>
    <main>
      @if (tela() === 'comparar') {
        <app-comparar />
      } @else {
        <app-benchmark />
      }
    </main>
  `
})
export class AppComponent {
  protected tela = signal<'comparar' | 'algoritmos'>('comparar');
}
