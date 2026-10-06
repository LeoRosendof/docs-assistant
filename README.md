# docs-assistant

Assistente de perguntas e respostas sobre documentos (RAG), construído como projeto de estudo
para praticar integração fullstack com IA.

> **Status:** em construção. O `ai-service` (RAG) está pronto e testado; o backend Java e o
> frontend ainda não foram começados. O histórico de commits é parte do projeto: mostra como
> cada decisão foi tomada, incluindo erros e correções.

## Arquitetura planejada

```
React (chat)  ->  Spring Boot (API REST)  ->  FastAPI (serviço de IA/RAG)  ->  LLM (Ollama local)
```

| Pasta          | Papel                                              | Stack                 | Situação      |
|----------------|----------------------------------------------------|-----------------------|---------------|
| `ai-service/`  | Embeddings, busca semântica, chamada ao LLM        | Python, FastAPI       | pronto        |
| `backend-java/`| Conversas, documentos, contrato REST com o frontend| Java, Spring Boot     | não iniciado  |
| `frontend/`    | Interface de chat                                  | React, yarn           | não iniciado  |

## O que o ai-service faz hoje

| Rota | O que faz |
|---|---|
| `POST /documentos` | Divide um texto em trechos, gera os embeddings e guarda na memória |
| `GET /documentos` | Lista os documentos indexados |
| `POST /buscar` | Devolve os trechos mais parecidos com uma pergunta, com o score de cada um |
| `POST /perguntar` | **RAG:** busca os trechos, monta o prompt com o contexto e responde citando as fontes |
| `POST /chat` | Pergunta direta ao modelo, sem documentos (serve de comparação) |
| `POST /chat/stream` | Igual ao `/chat`, com a resposta chegando aos poucos (SSE) |
| `GET /health` | Verifica se o serviço está de pé |

## Como rodar o ai-service

Pré-requisitos: Python 3.13 e o [Ollama](https://ollama.com) instalado.

```powershell
ollama pull llama3.1:8b     # modelo de linguagem (~4,9 GB)
ollama pull bge-m3          # modelo de embeddings (~1,2 GB)

cd ai-service
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe -m uvicorn app.main:app --port 8000
```

A documentação interativa fica em <http://localhost:8000/docs>. Os testes rodam sem o Ollama
(as chamadas são simuladas): `.\.venv\Scripts\python.exe -m pytest`.

Exemplo, com `curl`:

```bash
curl -X POST http://localhost:8000/documentos -H "Content-Type: application/json" \
  -d '{"nome": "manual", "texto": "Cada trabalho deve ser lido por pelo menos 2 pesquisadores."}'

curl -X POST http://localhost:8000/perguntar -H "Content-Type: application/json" \
  -d '{"pergunta": "Quantos pesquisadores devem ler cada trabalho?"}'
```

URL, modelos, timeout, tamanho dos trechos e score mínimo são configuráveis por variável de
ambiente; veja [`ai-service/.env.example`](ai-service/.env.example). O armazenamento é em
memória: ao reiniciar o serviço, os documentos precisam ser indexados de novo.

## Antes e depois do RAG

O mesmo modelo (`llama3.1:8b`), a mesma pergunta, com e sem o documento indexado. O documento
foi um PDF de 12 páginas com um modelo de revisão sistemática da literatura; as respostas
corretas eu conferi no próprio PDF. Execução de 06/10/2026.

| Pergunta | Sem RAG (`/chat`) | Com RAG (`/perguntar`) |
|---|---|---|
| Quais ferramentas serão usadas para gerir a equipe, a triagem e o aprofundamento? | Trello, Asana, Notion... | CADIMA, Rayyan e SciSpace |
| Quantos pesquisadores leem cada trabalho na triagem? | "2 a 4" | Pelo menos 2, e um 3º se houver divergência |
| Quantos pontos vale a resposta "Parcialmente"? | 1 ou 2 pontos (muda a cada execução) | 0,5 ponto |
| A partir de que ano os trabalhos são excluídos? | 2021 | Anteriores a 2014 |
| Quais são as strings de busca? | "não está disponível" | "Não encontrei essa informação" (a busca falhou) |
| Qual a capital da França? | Paris | "Não encontrei essa informação" (fora do documento) |

O modelo sem contexto responde com segurança coisas inventadas, e as respostas mudam a cada
execução. Com o contexto, ele passa a citar o documento e a recusar o que não encontra.

## Como medi a qualidade da busca

A primeira escolha de modelo de embeddings (`nomic-embed-text`) foi ruim em português: em 9
perguntas com resposta conhecida, só 2 acharam o trecho certo em 1º lugar. Troquei para o
`bge-m3`, que chegou a 5 de 9. A tabela completa, a hipótese e as limitações da medição estão
em [docs/DECISOES.md](docs/DECISOES.md) (decisão 004).

## Limitações conhecidas

- **Recuperação imperfeita:** a pergunta sobre as strings de busca não acha o trecho certo,
  porque a resposta está no meio de um trecho com assuntos misturados. O sistema recusa em vez
  de inventar, mas não responde.
- **Medição pequena:** são 9 perguntas e um único documento. Os números mostram uma tendência,
  não uma garantia.
- **Só texto:** não há leitura de PDF dentro do serviço; o texto precisa ser enviado já extraído.
  Texto riscado no PDF original vem como se fosse válido.
- **O modelo cita o que o documento diz, mesmo quando o próprio documento duvida.** O PDF escreve
  "anteriores ao ano de 2014 ?", com um ponto de interrogação, e a resposta apresenta o ano como
  fato.
- **Não responde perguntas gerais** (por escolha): fora do documento, ele recusa.
- **Armazenamento em memória** e **sem streaming** no `/perguntar`.

## Roadmap

1. [x] `ai-service`: chamada simples ao LLM -> configuração -> streaming -> tratamento de erros -> RAG
2. [ ] `backend-java`: CRUD, validação, erros padronizados, paginação, OpenAPI
3. [ ] `frontend` e `docker-compose`

Decisões e justificativas em [docs/DECISOES.md](docs/DECISOES.md).

## Uso de IA neste projeto

Parte do código foi escrita com apoio do Claude Code, por isso os commits trazem o
`Co-Authored-By`. Ainda sim, cada etapa é sistematicamente revisada e entendida antes de seguir para a próxima.
