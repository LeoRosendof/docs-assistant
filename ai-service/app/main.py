import json

import httpx
from fastapi import FastAPI
from fastapi.responses import StreamingResponse
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


def _eventos_sse(pergunta: str):
    """Repassa ao cliente cada pedaço de texto do Ollama como um evento SSE."""
    with httpx.stream(
        "POST",
        config.OLLAMA_URL,
        json={
            "model": config.OLLAMA_MODELO,
            "messages": [{"role": "user", "content": pergunta}],
            "stream": True,
        },
        timeout=config.OLLAMA_TIMEOUT,
    ) as resposta:
        resposta.raise_for_status()
        # O Ollama manda uma linha JSON por pedaço (NDJSON); a última tem done=true.
        for linha in resposta.iter_lines():
            if not linha:
                continue
            parte = json.loads(linha)
            if parte.get("done"):
                break
            # json.dumps escapa quebras de linha, que quebrariam o formato SSE;
            # ensure_ascii=False mantém os acentos legíveis no fluxo.
            texto = json.dumps(parte["message"]["content"], ensure_ascii=False)
            yield f"data: {texto}\n\n"
    yield "event: fim\ndata: {}\n\n"


@app.post("/chat/stream")
def chat_stream(req: ChatRequest):
    # Rota separada de /chat: quem não precisa de streaming continua com JSON simples.
    return StreamingResponse(_eventos_sse(req.pergunta), media_type="text/event-stream")
