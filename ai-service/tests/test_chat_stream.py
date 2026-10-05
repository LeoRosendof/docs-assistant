import json
from contextlib import contextmanager

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


class RespostaFalsa:
    def __init__(self, partes):
        self._linhas = [
            json.dumps({"message": {"role": "assistant", "content": p}, "done": False})
            for p in partes
        ]
        self._linhas.append(json.dumps({"message": {"content": ""}, "done": True}))

    def raise_for_status(self):
        pass

    def iter_lines(self):
        # Inclui uma linha vazia, como pode acontecer em um stream real.
        yield ""
        yield from self._linhas


def test_chat_stream_repassa_cada_pedaco_como_evento_sse(monkeypatch):
    @contextmanager
    def falso_stream(metodo, url, json, timeout):
        assert json["stream"] is True
        yield RespostaFalsa(["Olá", ", mundo\n", "!"])

    monkeypatch.setattr("app.main.httpx.stream", falso_stream)

    response = client.post("/chat/stream", json={"pergunta": "oi"})

    assert response.status_code == 200
    assert response.headers["content-type"].startswith("text/event-stream")
    assert response.text == (
        'data: "Olá"\n\n'
        'data: ", mundo\\n"\n\n'
        'data: "!"\n\n'
        'event: fim\ndata: {"truncada": false}\n\n'
    )
