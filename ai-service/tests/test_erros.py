import json
from contextlib import contextmanager

import httpx
import pytest
from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)
REQ = httpx.Request("POST", "http://ollama/api/chat")


def _post_que_falha(erro):
    def falso_post(url, json, timeout):
        raise erro

    return falso_post


@pytest.mark.parametrize(
    "erro, status",
    [
        (httpx.ConnectError("recusada", request=REQ), 503),
        (httpx.ReadTimeout("lento", request=REQ), 504),
    ],
)
def test_chat_traduz_falhas_de_conexao_e_timeout(monkeypatch, erro, status):
    monkeypatch.setattr("app.main.httpx.post", _post_que_falha(erro))

    response = client.post("/chat", json={"pergunta": "oi"})

    assert response.status_code == status
    assert response.json()["detail"]


def test_chat_devolve_502_quando_ollama_responde_erro(monkeypatch):
    # Ex.: modelo não baixado, o Ollama responde 404.
    def falso_post(url, json, timeout):
        return httpx.Response(404, json={"error": "model not found"}, request=REQ)

    monkeypatch.setattr("app.main.httpx.post", falso_post)

    response = client.post("/chat", json={"pergunta": "oi"})

    assert response.status_code == 502
    assert "404" in response.json()["detail"]


def test_chat_envia_limite_de_tokens_e_avisa_quando_trunca(monkeypatch):
    monkeypatch.setattr("app.main.config.OLLAMA_NUM_PREDICT", 42)
    enviado = {}

    def falso_post(url, json, timeout):
        enviado.update(json)
        return httpx.Response(
            200,
            json={"message": {"content": "cortada"}, "done_reason": "length"},
            request=REQ,
        )

    monkeypatch.setattr("app.main.httpx.post", falso_post)

    response = client.post("/chat", json={"pergunta": "oi"})

    assert enviado["options"] == {"num_predict": 42}
    assert response.json() == {"resposta": "cortada", "truncada": True}


def test_stream_devolve_503_quando_nao_consegue_conectar(monkeypatch):
    @contextmanager
    def falso_stream(metodo, url, json, timeout):
        raise httpx.ConnectError("recusada", request=REQ)
        yield

    monkeypatch.setattr("app.main.httpx.stream", falso_stream)

    response = client.post("/chat/stream", json={"pergunta": "oi"})

    assert response.status_code == 503


def test_stream_vira_evento_erro_quando_cai_no_meio(monkeypatch):
    class RespostaQueCai:
        def raise_for_status(self):
            pass

        def iter_lines(self):
            yield json.dumps({"message": {"content": "Olá"}, "done": False})
            raise httpx.ReadTimeout("lento", request=REQ)

    @contextmanager
    def falso_stream(metodo, url, json, timeout):
        yield RespostaQueCai()

    monkeypatch.setattr("app.main.httpx.stream", falso_stream)

    response = client.post("/chat/stream", json={"pergunta": "oi"})

    # O 200 já tinha sido enviado; o erro chega como evento, sem evento "fim".
    assert response.status_code == 200
    assert response.text.startswith('data: "Olá"\n\n')
    assert "event: erro" in response.text
    assert "demorou demais" in response.text
    assert "event: fim" not in response.text


def test_stream_avisa_truncada_no_evento_fim(monkeypatch):
    class RespostaCortada:
        def raise_for_status(self):
            pass

        def iter_lines(self):
            yield json.dumps({"message": {"content": "abc"}, "done": False})
            yield json.dumps({"message": {"content": ""}, "done": True, "done_reason": "length"})

    @contextmanager
    def falso_stream(metodo, url, json, timeout):
        yield RespostaCortada()

    monkeypatch.setattr("app.main.httpx.stream", falso_stream)

    response = client.post("/chat/stream", json={"pergunta": "oi"})

    assert response.text.endswith('event: fim\ndata: {"truncada": true}\n\n')
