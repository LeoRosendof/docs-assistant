import json
import uuid
from contextlib import ExitStack

import httpx
from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from app import config
from app.chunking import dividir_em_trechos
from app.embeddings import gerar_embeddings
from app.rag import MENSAGEM_SEM_RESPOSTA, montar_mensagens
from app.search import buscar_trechos
from app.store import armazenamento

app = FastAPI(title="docs-assistant ai-service")


class ChatRequest(BaseModel):
    pergunta: str


class DocumentoRequest(BaseModel):
    nome: str = Field(min_length=1)
    texto: str = Field(min_length=1)


class BuscaRequest(BaseModel):
    pergunta: str = Field(min_length=1)
    k: int = Field(default=3, ge=1, le=10)


class PerguntaRequest(BaseModel):
    pergunta: str = Field(min_length=1)
    k: int = Field(default=4, ge=1, le=10)


@app.get("/health")
def health():
    return {"status": "ok"}


def _payload(mensagens: list[dict], stream: bool, temperatura: float | None = None) -> dict:
    opcoes = {"num_predict": config.OLLAMA_NUM_PREDICT}
    if temperatura is not None:
        opcoes["temperature"] = temperatura
    return {
        "model": config.OLLAMA_MODELO,
        "messages": mensagens,
        "stream": stream,
        "options": opcoes,
    }


def _mensagem_do_usuario(pergunta: str) -> list[dict]:
    return [{"role": "user", "content": pergunta}]


def _erro_http(exc: httpx.HTTPError) -> HTTPException:
    """Traduz falhas do Ollama para o status HTTP que o cliente deve ver."""
    if isinstance(exc, httpx.TimeoutException):
        return HTTPException(504, "O modelo demorou demais para responder.")
    if isinstance(exc, httpx.ConnectError):
        return HTTPException(503, "Não foi possível conectar ao Ollama. Ele está rodando?")
    if isinstance(exc, httpx.HTTPStatusError):
        return HTTPException(502, f"O Ollama respondeu com erro {exc.response.status_code}.")
    return HTTPException(502, "Falha na comunicação com o Ollama.")


def _completar(mensagens: list[dict], temperatura: float | None = None) -> dict:
    """Pede ao Ollama uma resposta completa e devolve o texto e se foi cortada."""
    # O timeout padrão do httpx (5 s) estoura na primeira chamada, quando o modelo
    # ainda está carregando na VRAM (~35 s), por isso o padrão é folgado.
    try:
        resposta = httpx.post(
            config.OLLAMA_URL,
            json=_payload(mensagens, stream=False, temperatura=temperatura),
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


@app.post("/chat")
def chat(req: ChatRequest):
    return _completar(_mensagem_do_usuario(req.pergunta))


@app.post("/documentos", status_code=201)
def indexar_documento(req: DocumentoRequest):
    trechos = dividir_em_trechos(req.texto, config.CHUNK_TAMANHO, config.CHUNK_SOBREPOSICAO)
    if not trechos:
        raise HTTPException(422, "O texto do documento está vazio.")
    try:
        vetores = gerar_embeddings(trechos)
    except httpx.HTTPError as exc:
        raise _erro_http(exc) from exc
    documento_id = str(uuid.uuid4())
    armazenamento.adicionar(documento_id, req.nome, trechos, vetores)
    return {"id": documento_id, "nome": req.nome, "trechos": len(trechos)}


@app.get("/documentos")
def listar_documentos():
    return armazenamento.resumo_por_documento()


def _recuperar(pergunta: str, k: int):
    """Os k trechos indexados mais parecidos com a pergunta, com seus scores."""
    if not armazenamento.trechos:
        return []
    try:
        # A pergunta pode usar um prefixo diferente do usado nos trechos (conforme o modelo).
        [vetor] = gerar_embeddings([pergunta], prefixo=config.EMBED_PREFIXO_PERGUNTA)
    except httpx.HTTPError as exc:
        raise _erro_http(exc) from exc
    return buscar_trechos(vetor, armazenamento.trechos, k)


def _fonte(trecho, score: float) -> dict:
    return {
        "documento": trecho.documento_nome,
        "posicao": trecho.posicao,
        "texto": trecho.texto,
        "score": round(score, 4),
    }


@app.post("/buscar")
def buscar(req: BuscaRequest):
    return {"resultados": [_fonte(t, s) for t, s in _recuperar(req.pergunta, req.k)]}


@app.post("/perguntar")
def perguntar(req: PerguntaRequest):
    relevantes = [
        (t, s) for t, s in _recuperar(req.pergunta, req.k) if s >= config.RAG_SCORE_MINIMO
    ]
    if not relevantes:
        # Sem trecho relevante, chamar o modelo só abriria espaço para ele inventar.
        return {"resposta": MENSAGEM_SEM_RESPOSTA, "fontes": [], "truncada": False}
    resultado = _completar(
        montar_mensagens(req.pergunta, [t for t, _ in relevantes]),
        temperatura=config.RAG_TEMPERATURA,
    )
    if resultado["resposta"].strip().startswith(MENSAGEM_SEM_RESPOSTA):
        # O modelo recusou: os trechos recuperados não serviram, então não são fontes.
        return {**resultado, "fontes": []}
    return {**resultado, "fontes": [_fonte(t, s) for t, s in relevantes]}


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
                json=_payload(_mensagem_do_usuario(req.pergunta), stream=True),
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
