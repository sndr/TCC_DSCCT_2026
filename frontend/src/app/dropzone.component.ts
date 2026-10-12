import { Component, input, output, signal } from '@angular/core';

@Component({
  selector: 'app-dropzone',
  standalone: true,
  template: `
    <label class="dropzone"
           [class.ativo]="arrastando()"
           [class.preenchido]="!!arquivoAtual()"
           (dragover)="aoArrastar($event)"
           (dragleave)="arrastando.set(false)"
           (drop)="aoSoltar($event)">
      <input type="file" [accept]="aceitar" [multiple]="multiplo()" (change)="aoEscolher($event)">
      <span class="dz-titulo">{{ titulo() }}</span>
      @if (arquivoAtual(); as arquivo) {
        <span class="dz-arquivo">{{ arquivo.name }}</span>
        <span class="dz-dica">{{ tamanho(arquivo.size) }} · clique para trocar</span>
      } @else {
        <span class="dz-dica">{{ multiplo() ? 'Arraste os arquivos ou clique para escolher' : 'Arraste um arquivo ou clique para escolher' }}</span>
        <span class="dz-dica">PDF, DOCX, TXT, ODT, RTF</span>
      }
    </label>
  `
})
export class DropzoneComponent {
  titulo = input.required<string>();
  multiplo = input(false);
  arquivoAtual = input<File | null>(null);
  escolhidos = output<File[]>();

  protected arrastando = signal(false);
  protected readonly aceitar = '.pdf,.docx,.doc,.txt,.odt,.rtf,.md';

  protected aoArrastar(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastando.set(true);
  }

  protected aoSoltar(evento: DragEvent): void {
    evento.preventDefault();
    this.arrastando.set(false);
    const arquivos = Array.from(evento.dataTransfer?.files ?? []);
    if (arquivos.length) {
      this.escolhidos.emit(this.multiplo() ? arquivos : [arquivos[0]]);
    }
  }

  protected aoEscolher(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const arquivos = Array.from(entrada.files ?? []);
    if (arquivos.length) {
      this.escolhidos.emit(arquivos);
    }
    entrada.value = '';
  }

  protected tamanho(bytes: number): string {
    if (bytes < 1024 * 1024) {
      return `${Math.max(1, Math.round(bytes / 1024))} KB`;
    }
    return `${(bytes / (1024 * 1024)).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} MB`;
  }
}
