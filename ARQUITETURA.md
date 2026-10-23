# Arquitetura do projeto

O sistema é uma aplicação web em duas partes: uma interface Angular envia documentos para uma API Spring Boot, que extrai e prepara os textos, executa os algoritmos de similaridade e persiste o histórico das comparações individuais.

## Visão geral

```mermaid
flowchart LR
    U[Usuário] -->|seleciona arquivos e consulta resultados| FE

    subgraph FRONT[Frontend — Angular 19]
      FE[AppComponent e telas]
      TELAS[Comparar · Benchmark · Resultados]
      API[ApiService · HttpClient]
      FE --> TELAS --> API
    end

    API -->|HTTP /api · multipart/form-data| BE
    subgraph BACK[Backend — Spring Boot 3 · Java 21]
      BE[ComparacaoController · REST]
      ERR[TratadorDeErros · erros HTTP]
      ORQ[ComparacaoService]
      BENCH[BenchmarkService]
      EXT[ExtracaoTextoService · Apache Tika]
      PREP[PreparacaoDocumentoService]
      CORE[Core Java · tokenização, parágrafos, vetores e matching]
      TF[TF-IDF · cosseno]
      EMB[EmbeddingsSimilaridadeService]
      DJL[EmbeddingService · DJL / PyTorch]
      DTO[DTOs de resposta]
      REPO[ComparacaoRepository · Spring Data JPA]
      ENTITY[Entidade Comparacao]

      BE --> ORQ
      BE --> BENCH
      BE --> DTO
      BE -. exceções .-> ERR
      ORQ --> EXT --> PREP --> CORE
      BENCH --> EXT
      BENCH --> PREP
      ORQ --> TF
      BENCH --> TF
      ORQ --> EMB
      BENCH --> EMB
      EMB --> DJL
      TF --> CORE
      EMB --> CORE
      ORQ --> REPO --> ENTITY
      BENCH --> DTO
      ORQ --> DTO
    end

    subgraph IA[Modelo de linguagem vetorial]
      MODEL[Hugging Face\nparaphrase-multilingual-MiniLM-L12-v2]
      CACHE[Cache local DJL\nmodelo baixado no primeiro uso]
      MODEL --> CACHE
    end
    DJL -->|carrega/inferência| CACHE

    REPO -->|JPA| DB[(Banco relacional)]
    DB -. padrão .-> H2[(H2 em arquivo\nbackend/data/comparador)]
    DB -. perfil postgres .-> PG[(PostgreSQL 16\nDocker Compose)]

    API -. proxy de desenvolvimento /api .-> BE
```

## Fluxo de comparação

```mermaid
sequenceDiagram
    actor Pessoa as Usuário
    participant UI as Angular / ApiService
    participant C as ComparacaoController
    participant S as ComparacaoService
    participant T as Apache Tika
    participant P as PreparacaoDocumentoService + core
    participant A as Serviços TF-IDF / Embeddings
    participant R as Repositório JPA

    Pessoa->>UI: Envia arquivo A, arquivo B e método
    UI->>C: POST /api/comparacoes (multipart)
    C->>S: comparar(A, B, método)
    S->>T: Extrai texto dos documentos
    T-->>S: Texto extraído
    S->>P: Divide em parágrafos e gera tokens
    P-->>S: Documentos preparados
    S->>A: Executa TF-IDF, embeddings ou ambos
    A-->>S: Similaridade, palavras e parágrafos relacionados
    opt Resultado válido da comparação individual
        S->>R: Salva histórico (falha não impede a resposta)
    end
    S-->>C: DTO com métricas e resultados
    C-->>UI: JSON
    UI-->>Pessoa: Exibe resultado
```

## Responsabilidades principais

- **Frontend:** `AppComponent` seleciona as telas de comparação, benchmark e resultados. `ApiService` chama `/api/status`, `/api/comparacoes` e `/api/benchmark`; o proxy do Angular encaminha `/api` para `localhost:8080` durante o desenvolvimento.
- **API e orquestração:** `ComparacaoController` expõe os endpoints REST. `ComparacaoService` prepara dois documentos e executa o(s) método(s) escolhido(s). `BenchmarkService` recebe de 2 a 6 documentos, calcula todos os pares e resume médias; embeddings são gerados uma vez por documento nesse fluxo.
- **Extração e preparação:** Apache Tika extrai texto de formatos suportados. `PreparacaoDocumentoService` separa parágrafos e aplica o pré-processamento/tokenização do pacote `core`.
- **Algoritmos:** TF-IDF e cosseno usam vetores esparsos implementados no projeto. Embeddings usam DJL com PyTorch e o modelo multilíngue do Hugging Face; o download ocorre no primeiro uso. O pacote `core` também contém divisão de parágrafos, matching e operações com vetores densos.
- **Persistência:** comparações individuais bem-sucedidas são guardadas pela entidade JPA `Comparacao`; o histórico retorna as 30 mais recentes. O benchmark não grava histórico. O banco padrão real da configuração é H2 em arquivo; PostgreSQL 16 é alternativa pelo perfil `postgres` e pelo `docker-compose.yml`.

## Endpoints expostos

| Método | Rota | Função |
|---|---|---|
| `GET` | `/api/status` | Informa se embeddings estão habilitados e o modelo configurado. |
| `POST` | `/api/comparacoes` | Compara dois arquivos; método `TFIDF`, `EMBEDDINGS` ou `AMBOS`. |
| `GET` | `/api/comparacoes` | Lista o histórico recente. |
| `DELETE` | `/api/comparacoes` | Limpa o histórico. |
| `POST` | `/api/benchmark` | Compara todos os pares de 2 a 6 arquivos pelos métodos disponíveis. |

