import httpx
import pytest
from fastapi.testclient import TestClient

from app.embeddings import PREFIXO_DOCUMENTO, gerar_embeddings
from app.main import app
from app.store import ArmazenamentoTrechos

client = TestClient(app)
REQ = httpx.Request("POST", "http://ollama/api/embed")


@pytest.fixture(autouse=True)
def armazenamento_limpo(monkeypatch):
    # Cada teste começa com um armazenamento vazio, sem depender dos outros.
    monkeypatch.setattr("app.main.armazenamento", ArmazenamentoTrechos())


def test_gerar_embeddings_envia_prefixo_e_modelo_e_devolve_um_vetor_por_texto(monkeypatch):
    monkeypatch.setattr("app.embeddings.config.OLLAMA_EMBED_MODELO", "modelo-x")
    enviado = {}

    def falso_post(url, json, timeout):
        enviado.update(json)
        return httpx.Response(200, json={"embeddings": [[1.0, 0.0], [0.0, 1.0]]}, request=REQ)

    monkeypatch.setattr("app.embeddings.httpx.post", falso_post)

    vetores = gerar_embeddings(["a", "b"])

    assert vetores == [[1.0, 0.0], [0.0, 1.0]]
    assert enviado["model"] == "modelo-x"
    assert enviado["input"] == [PREFIXO_DOCUMENTO + "a", PREFIXO_DOCUMENTO + "b"]


def test_indexar_documento_guarda_um_vetor_por_trecho(monkeypatch):
    chamadas = []

    def falso_gerar(textos):
        chamadas.append(textos)
        return [[float(i)] for i, _ in enumerate(textos)]

    monkeypatch.setattr("app.main.gerar_embeddings", falso_gerar)
    texto = " ".join(f"palavra{i}" for i in range(300))

    response = client.post("/documentos", json={"nome": "manual.txt", "texto": texto})

    assert response.status_code == 201
    corpo = response.json()
    assert corpo["nome"] == "manual.txt"
    assert corpo["trechos"] == len(chamadas[0]) > 1
    assert client.get("/documentos").json() == [
        {"id": corpo["id"], "nome": "manual.txt", "trechos": corpo["trechos"]}
    ]


def test_indexar_documento_so_com_espacos_devolve_422(monkeypatch):
    monkeypatch.setattr("app.main.gerar_embeddings", lambda textos: pytest.fail("não deveria chamar"))

    response = client.post("/documentos", json={"nome": "vazio", "texto": "   \n "})

    assert response.status_code == 422


def test_indexar_documento_sem_nome_devolve_422():
    response = client.post("/documentos", json={"nome": "", "texto": "conteúdo"})

    assert response.status_code == 422


def test_indexar_documento_devolve_503_e_nao_guarda_nada_se_ollama_cair(monkeypatch):
    def falha(textos):
        raise httpx.ConnectError("recusada", request=REQ)

    monkeypatch.setattr("app.main.gerar_embeddings", falha)

    response = client.post("/documentos", json={"nome": "a", "texto": "conteúdo"})

    assert response.status_code == 503
    assert client.get("/documentos").json() == []
