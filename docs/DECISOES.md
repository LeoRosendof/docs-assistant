# Decisões de projeto

Registro curto de cada decisão e do motivo. Atualizado ao longo dos commits.

## 001 - Monorepo
Os três serviços ficam no mesmo repositório. O objetivo é mostrar a integração entre eles,
e um único `docker-compose` sobe tudo.

## 002 - LLM local com Ollama
Roda na GPU local (RTX 3060, 12 GB), sem custo e sem chave de API. O provedor será
configurável por variável de ambiente para permitir trocar por APIs externas.

## 003 - RAG primeiro
A parte de IA é a mais difícil de explicar e a mais valiosa, então o `ai-service` vem
antes do backend Java.
