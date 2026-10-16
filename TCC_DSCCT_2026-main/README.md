# Comparador de documentos (TCC)

Angular (front-end) + Spring Boot / Java 21 (back-end) + PostgreSQL.
Compara dois documentos (PDF, DOCX, TXT, ODT, RTF) com **TF-IDF + similaridade de cosseno** e com **embeddings**
(modelo multilíngue rodando em Java via Deep Java Library), e permite avaliar os dois algoritmos lado a lado.

## Requisitos
- Java 21+ e Maven 3.9+
- Node 20+ e npm
- Docker (opcional, só para o PostgreSQL)

## Como rodar

### 1. Banco de dados
```bash
docker compose up -d
```
Sem Docker? Use o perfil `h2` no passo 2 (banco em memória, os dados somem ao reiniciar).

### 2. Back-end (porta 8080)
```bash
cd backend
mvn spring-boot:run
# ou, sem PostgreSQL:
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

### 3. Front-end (porta 4200)
```bash
cd frontend
npm install
npm start
```
Abra http://localhost:4200. O `proxy.conf.json` encaminha `/api` para o back-end.

## Telas
- **Comparar dois documentos**: upload de A e B, escolha do método (TF-IDF, Embeddings ou os dois), percentual de
  similaridade, palavras semelhantes, parágrafos relacionados (com as palavras em comum destacadas), tempo de execução
  e histórico salvo no banco.
- **Comparação dos algoritmos**: envie de 2 a 6 documentos; o sistema compara todos os pares com os dois métodos e mostra
  a tabela (Documento 1 × 2, 1 × 3...), médias de similaridade, tempo médio por par e um gráfico.

## API
| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/comparacoes` | multipart: `arquivoA`, `arquivoB`, `metodo` (`TFIDF`, `EMBEDDINGS`, `AMBOS`) |
| GET  | `/api/comparacoes` | últimas 30 comparações salvas |
| POST | `/api/benchmark` | multipart: `arquivos` (2 a 6) |
| GET  | `/api/status` | informa se os embeddings estão ativados e qual modelo |

## Embeddings: o que esperar
- No **primeiro uso**, o back-end baixa o modelo `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2`
  (~470 MB) e as bibliotecas nativas do PyTorch. Precisa de internet e pode levar alguns minutos.
- Depois disso fica em cache (`~/.djl.ai`) e funciona offline.
- Para desligar: `app.embeddings.enabled=false` em `application.yml`. O TF-IDF continua funcionando.
- O tempo reportado para embeddings inclui a geração dos vetores dos dois documentos.

## Estrutura do back-end
```
core/      algoritmos em Java puro, sem Spring (TfIdfModel, TextPreprocessor, ParagraphSplitter,
           ParagraphMatcher, DenseVectors, LexicalOverlap), fáceis de citar e testar no TCC
service/   Tika, TF-IDF, embeddings (DJL), orquestração e benchmark
web/       controllers REST, DTOs e tratamento de erros
domain/    entidade JPA (histórico) e repositório
```

## Detalhes do algoritmo TF-IDF
- Pré-processamento: minúsculas, remoção de acentos, tokenização por letras, remoção de stopwords PT/EN e de tokens < 3 letras.
- TF = 1 + ln(contagem); IDF suavizado = ln((1+N)/(1+df)) + 1, com N = parágrafos dos dois documentos.
- Similaridade = cosseno entre os vetores TF-IDF dos documentos inteiros.
- Parágrafos relacionados: para cada parágrafo de A, o melhor correspondente em B com cosseno ≥ 0,30 (embeddings: ≥ 0,60).
  Os limiares ficam em `application.yml` (`app.similarity.*`).

## Testes
```bash
cd backend && mvn test
```
