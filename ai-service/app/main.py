import httpx
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="docs-assistant ai-service")

# Primeira versão: tudo fixo no código. Vira variável de ambiente no próximo commit.
OLLAMA_URL = "http://localhost:11434/api/chat"
MODELO = "llama3.1:8b"


class ChatRequest(BaseModel):
    pergunta: str


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/chat")
def chat(req: ChatRequest):
    # O timeout padrão do httpx (5 s) estoura na primeira chamada, quando o modelo
    # ainda está carregando na VRAM (~35 s). Por enquanto um valor fixo e folgado.
    resposta = httpx.post(
        OLLAMA_URL,
        json={
            "model": MODELO,
            "messages": [{"role": "user", "content": req.pergunta}],
            "stream": False,
        },
        timeout=120,
    )
    resposta.raise_for_status()
    return {"resposta": resposta.json()["message"]["content"]}
