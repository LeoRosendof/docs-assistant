import json
from contextlib import ExitStack

import httpx
from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

from app import config

app = FastAPI(title="docs-assistant ai-service")


class ChatRequest(BaseModel):
    pergunta: str


@app.get("/health")
def health():
    return {"status": "ok"}


def _payload(pergunta: str, stream: bool) -> dict:
    return {
        "model": config.OLLAMA_MODELO,
        "messages": [{"role": "user", "content": pergunta}],
        "stream": stream,
        "options": {"num_predict": config.OLLAMA_NUM_PREDICT},
    }


def _erro_http(exc: httpx.HTTPError) -> HTTPException:
    """Traduz falhas do Ollama para o status HTTP que o cliente deve ver."""
    if isinstance(exc, httpx.TimeoutException):
        return HTTPException(504, "O modelo demorou demais para responder.")
    if isinstance(exc, httpx.ConnectError):
        return HTTPException(503, "Não foi possível conectar ao Ollama. Ele está rodando?")
    if isinstance(exc, httpx.HTTPStatusError):
        return HTTPException(502, f"O Ollama respondeu com erro {exc.response.status_code}.")
    return HTTPException(502, "Falha na comunicação com o Ollama.")


@app.post("/chat")
def chat(req: ChatRequest):
    # O timeout padrão do httpx (5 s) estoura na primeira chamada, quando o modelo
    # ainda está carregando na VRAM (~35 s), por isso o padrão é folgado.
    try:
        resposta = httpx.post(
            config.OLLAMA_URL,
            json=_payload(req.pergunta, stream=False),
            timeout=config.OLLAMA_TIMEOUT,
        )
        resposta.raise_for_status()
    except httpx.HTTPError as exc:
        raise _erro_http(exc) from exc
    dados = resposta.json()
    return {
        "resposta": dados["message"]["content"],
        # done_reason "length" significa que o num_predict cortou a resposta.
        "truncada": dados.get("done_reason") == "length",
    }


def _eventos_sse(resposta: httpx.Response, conexao: ExitStack):
    """Repassa ao cliente cada pedaço de texto do Ollama como um evento SSE."""
    truncada = False
    try:
        # O Ollama manda uma linha JSON por pedaço (NDJSON); a última tem done=true.
        for linha in resposta.iter_lines():
            if not linha:
                continue
            parte = json.loads(linha)
            if parte.get("done"):
                truncada = parte.get("done_reason") == "length"
                break
            # json.dumps escapa quebras de linha, que quebrariam o formato SSE;
            # ensure_ascii=False mantém os acentos legíveis no fluxo.
            texto = json.dumps(parte["message"]["content"], ensure_ascii=False)
            yield f"data: {texto}\n\n"
    except httpx.HTTPError as exc:
        # O status 200 já foi enviado, então o erro só pode ir como evento.
        detalhe = json.dumps({"mensagem": _erro_http(exc).detail}, ensure_ascii=False)
        yield f"event: erro\ndata: {detalhe}\n\n"
        return
    finally:
        conexao.close()
    yield f"event: fim\ndata: {json.dumps({'truncada': truncada})}\n\n"


@app.post("/chat/stream")
def chat_stream(req: ChatRequest):
    # Rota separada de /chat: quem não precisa de streaming continua com JSON simples.
    # A conexão é aberta aqui, antes de devolver a resposta, para que falhas de
    # conexão ainda virem um status HTTP correto (503, 504, 502).
    conexao = ExitStack()
    try:
        resposta = conexao.enter_context(
            httpx.stream(
                "POST",
                config.OLLAMA_URL,
                json=_payload(req.pergunta, stream=True),
                timeout=config.OLLAMA_TIMEOUT,
            )
        )
        resposta.raise_for_status()
    except httpx.HTTPError as exc:
        conexao.close()
        raise _erro_http(exc) from exc
    return StreamingResponse(
        _eventos_sse(resposta, conexao), media_type="text/event-stream"
    )
