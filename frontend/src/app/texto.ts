export interface Trecho {
  texto: string;
  destaque: boolean;
}

export function normalizar(palavra: string): string {
  return palavra.normalize('NFD').replace(/\p{M}+/gu, '').toLowerCase();
}

export function destacar(texto: string, palavras: Set<string>): Trecho[] {
  return texto
    .split(/(\p{L}+)/u)
    .filter(parte => parte.length > 0)
    .map(parte => ({
      texto: parte,
      destaque: /^\p{L}+$/u.test(parte) && palavras.has(normalizar(parte))
    }));
}

export function percentual(valor: number | null | undefined): string {
  if (valor === null || valor === undefined) {
    return '—';
  }
  return (valor * 100).toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }) + '%';
}

export function milissegundos(valor: number | null | undefined): string {
  if (valor === null || valor === undefined) {
    return '—';
  }
  return valor.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }) + ' ms';
}

export function nomeDoMetodo(metodo: string): string {
  return metodo === 'EMBEDDINGS' ? 'Embeddings' : metodo;
}
