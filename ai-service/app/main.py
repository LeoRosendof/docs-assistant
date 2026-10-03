import httpx
from fastapi import FastAPI
from pydantic import BaseModel

from app import config

app = FastAPI(title="docs-assistant ai-service")


class ChatRequest(BaseModel):
    pergunta: str


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/chat")
def chat(req: ChatRequest):
    # O timeout padrão do httpx (5 s) estoura na primeira chamada, quando o modelo
    # ainda está carregando na VRAM (~35 s), por isso o padrão é folgado.
    resposta = httpx.post(
        config.OLLAMA_URL,
        json={
            "model": config.OLLAMA_MODELO,
            "messages": [{"role": "user", "content": req.pergunta}],
            "stream": False,
        },
        timeout=config.OLLAMA_TIMEOUT,
    )
    resposta.raise_for_status()
    return {"resposta": resposta.json()["message"]["content"]}
