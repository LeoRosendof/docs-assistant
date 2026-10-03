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
    assert response.json() == {"resposta": "resposta de teste"}
