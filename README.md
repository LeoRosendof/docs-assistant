# docs-assistant

Assistente de perguntas e respostas sobre documentos (RAG), construído como projeto de estudo
para praticar integração fullstack com IA.

> **Status:** em construção. O histórico de commits é parte do projeto: mostra como cada
> decisão foi tomada, incluindo erros e correções.

## Arquitetura planejada

```
React (chat)  ->  Spring Boot (API REST)  ->  FastAPI (serviço de IA/RAG)  ->  LLM (Ollama local)
```

| Pasta          | Papel                                              | Stack                 |
|----------------|----------------------------------------------------|-----------------------|
| `ai-service/`  | Embeddings, busca semântica, chamada ao LLM        | Python, FastAPI       |
| `backend-java/`| Conversas, documentos, contrato REST com o frontend| Java, Spring Boot     |
| `frontend/`    | Interface de chat                                  | React, yarn           |

## Roadmap

1. `ai-service`: chamada simples ao LLM -> streaming -> tratamento de erros -> RAG
2. `backend-java`: CRUD, validação, erros padronizados, paginação, OpenAPI
3. `frontend` e `docker-compose`

Decisões e justificativas em [docs/DECISOES.md](docs/DECISOES.md).

## Uso de IA neste projeto

Parte do código foi escrita com apoio do Claude Code. Os commits trazem o trailer
`Co-Authored-By`. Cada etapa é revisada e entendida antes de seguir para a próxima.
