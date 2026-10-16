import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { BenchmarkResponse, ComparacaoResponse, HistoricoItem, Metodo, Status } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  status(): Observable<Status> {
    return this.http.get<Status>(`${this.base}/status`);
  }

  comparar(arquivoA: File, arquivoB: File, metodo: Metodo): Observable<ComparacaoResponse> {
    const form = new FormData();
    form.append('arquivoA', arquivoA);
    form.append('arquivoB', arquivoB);
    form.append('metodo', metodo);
    return this.http.post<ComparacaoResponse>(`${this.base}/comparacoes`, form);
  }

  historico(): Observable<HistoricoItem[]> {
    return this.http.get<HistoricoItem[]>(`${this.base}/comparacoes`);
  }

  benchmark(arquivos: File[]): Observable<BenchmarkResponse> {
    const form = new FormData();
    arquivos.forEach(arquivo => form.append('arquivos', arquivo));
    return this.http.post<BenchmarkResponse>(`${this.base}/benchmark`, form);
  }
}

export function mensagemDeErro(e: unknown): string {
  if (e instanceof HttpErrorResponse) {
    if (e.status === 0) {
      return 'Não foi possível conectar ao back-end. Confirme que o Spring Boot está rodando na porta 8080.';
    }
    const detalhe = (e.error && typeof e.error === 'object' && 'erro' in e.error) ? (e.error as { erro: string }).erro : null;
    if (detalhe) {
      return detalhe;
    }
    // 5xx sem o JSON do nosso back-end: quem respondeu foi o proxy do Angular, ou seja, o Spring Boot não está no ar.
    if (e.status >= 500) {
      return 'O back-end não respondeu. Confirme que o Spring Boot subiu (porta 8080) e abra http://localhost:8080/api/status no navegador para testar. Se ele não subir, veja o erro no terminal.';
    }
    return `Erro ${e.status} ao processar a requisição.`;
  }
  return 'Erro inesperado.';
}