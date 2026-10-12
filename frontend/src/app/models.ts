export type Metodo = 'TFIDF' | 'EMBEDDINGS' | 'AMBOS';

export interface ParagrafoRelacionado {
  textoA: string;
  textoB: string;
  similaridade: number;
}

export interface ResultadoMetodo {
  metodo: string;
  similaridade: number | null;
  palavrasSemelhantes: number | null;
  paragrafosRelacionados: number | null;
  tempoMs: number | null;
  palavrasEmComum: string[];
  paragrafos: ParagrafoRelacionado[];
  erro: string | null;
}

export interface ComparacaoResponse {
  arquivoA: string;
  arquivoB: string;
  paragrafosA: number;
  paragrafosB: number;
  palavrasA: number;
  palavrasB: number;
  resultados: ResultadoMetodo[];
}

export interface ParBenchmark {
  indiceA: number;
  indiceB: number;
  tfidf: number | null;
  tfidfMs: number | null;
  embeddings: number | null;
  embeddingsMs: number | null;
}

export interface ResumoMetodo {
  metodo: string;
  similaridadeMedia: number | null;
  tempoMedioMs: number | null;
}

export interface BenchmarkResponse {
  documentos: string[];
  pares: ParBenchmark[];
  resumo: ResumoMetodo[];
  avisoEmbeddings: string | null;
}

export interface HistoricoItem {
  id: number;
  arquivoA: string;
  arquivoB: string;
  metodo: string;
  similaridade: number;
  palavrasSemelhantes: number;
  paragrafosRelacionados: number;
  tempoMs: number;
  criadoEm: string;
}

export interface Status {
  embeddingsAtivados: boolean;
  modeloEmbeddings: string;
}
