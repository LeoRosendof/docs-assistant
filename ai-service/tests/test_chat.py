import httpx
from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_chat_devolve_resposta_do_modelo(monkeypatch):
    # Não chama o Ollama de verdade: o teste não pode depender de GPU nem de rede.
    def falso_post(url, json, timeout):
        assert json["messages"][-1]["content"] == "O que é RAG?"
        return httpx.Response(
            200,
            json={"message": {"role": "assistant", "content": "resposta de teste"}},
            request=httpx.Request("POST", url),
        )

    monkeypatch.setattr("app.main.httpx.post", falso_post)

    response = client.post("/chat", json={"pergunta": "O que é RAG?"})

    assert response.status_code == 200
    assert response.json() == {"resposta": "resposta de teste", "truncada": False}


def test_chat_usa_url_modelo_e_timeout_da_configuracao(monkeypatch):
    monkeypatch.setattr("app.main.config.OLLAMA_URL", "http://outro-host:1234/api/chat")
    monkeypatch.setattr("app.main.config.OLLAMA_MODELO", "outro-modelo")
    monkeypatch.setattr("app.main.config.OLLAMA_TIMEOUT", 7.0)
    recebido = {}

    def falso_post(url, json, timeout):
        recebido.update(url=url, modelo=json["model"], timeout=timeout)
        return httpx.Response(
            200,
            json={"message": {"role": "assistant", "content": "ok"}},
            request=httpx.Request("POST", url),
        )

    monkeypatch.setattr("app.main.httpx.post", falso_post)

    client.post("/chat", json={"pergunta": "oi"})

    assert recebido == {
        "url": "http://outro-host:1234/api/chat",
        "modelo": "outro-modelo",
        "timeout": 7.0,
    }
