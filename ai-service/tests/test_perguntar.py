import httpx
import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.rag import MENSAGEM_SEM_RESPOSTA, montar_mensagens
from app.store import ArmazenamentoTrechos, Trecho

client = TestClient(app)
REQ = httpx.Request("POST", "http://ollama/api/chat")


@pytest.fixture(autouse=True)
def configuracao_fixa(monkeypatch):
    monkeypatch.setattr("app.main.config.RAG_SCORE_MINIMO", 0.40)
    monkeypatch.setattr("app.main.config.RAG_TEMPERATURA", 0.0)


@pytest.fixture
def armazenamento_com_rag_e_gatos(monkeypatch):
    armazenamento = ArmazenamentoTrechos()
    armazenamento.adicionar("d1", "rag.txt", ["RAG busca trechos e depois gera a resposta."], [[1.0, 0.0]])
    armazenamento.adicionar("d2", "gatos.txt", ["Gatos dormem muito."], [[0.0, 1.0]])
    monkeypatch.setattr("app.main.armazenamento", armazenamento)


def _chat_que_responde(monkeypatch, texto="resposta do modelo", done_reason="stop"):
    enviado = {}

    def falso_post(url, json, timeout):
        enviado.update(json)
        return httpx.Response(
            200,
            json={"message": {"content": texto}, "done_reason": done_reason},
            request=REQ,
        )

    monkeypatch.setattr("app.main.httpx.post", falso_post)
    return enviado


def test_montar_mensagens_numera_os_trechos_e_poe_a_pergunta_por_ultimo():
    trechos = [
        Trecho("d1", "a.txt", 0, "texto A", [0.0]),
        Trecho("d2", "b.txt", 3, "texto B", [0.0]),
    ]

    sistema, usuario = montar_mensagens("Qual a dúvida?", trechos)

    assert sistema["role"] == "system"
    assert MENSAGEM_SEM_RESPOSTA in sistema["content"]
    assert usuario["role"] == "user"
    assert "[1] (a.txt, trecho 0)\ntexto A" in usuario["content"]
    assert "[2] (b.txt, trecho 3)\ntexto B" in usuario["content"]
    assert usuario["content"].endswith("Pergunta: Qual a dúvida?")


def test_perguntar_usa_so_os_trechos_relevantes_e_devolve_as_fontes(
    monkeypatch, armazenamento_com_rag_e_gatos
):
    monkeypatch.setattr("app.main.gerar_embeddings", lambda textos, prefixo: [[1.0, 0.0]])
    enviado = _chat_que_responde(monkeypatch, "RAG busca e gera [1].")

    response = client.post("/perguntar", json={"pergunta": "O que é RAG?"})

    corpo = response.json()
    assert response.status_code == 200
    assert corpo["resposta"] == "RAG busca e gera [1]."
    assert corpo["truncada"] is False
    assert [f["documento"] for f in corpo["fontes"]] == ["rag.txt"]
    assert corpo["fontes"][0]["score"] == 1.0
    contexto = enviado["messages"][1]["content"]
    assert "RAG busca trechos" in contexto
    assert "Gatos dormem" not in contexto  # trecho irrelevante não entra no prompt
    assert enviado["options"]["temperature"] == 0.0


def test_perguntar_sem_fontes_quando_o_proprio_modelo_recusa(monkeypatch, armazenamento_com_rag_e_gatos):
    # Os trechos passaram do score mínimo, mas o modelo concluiu que não respondem à pergunta.
    monkeypatch.setattr("app.main.gerar_embeddings", lambda textos, prefixo: [[1.0, 0.0]])
    _chat_que_responde(monkeypatch, MENSAGEM_SEM_RESPOSTA)

    corpo = client.post("/perguntar", json={"pergunta": "x"}).json()

    assert corpo["resposta"] == MENSAGEM_SEM_RESPOSTA
    assert corpo["fontes"] == []


def test_perguntar_sem_trecho_relevante_nao_chama_o_modelo(monkeypatch, armazenamento_com_rag_e_gatos):
    # Vetor oposto ao do RAG e ortogonal ao dos gatos: scores -1 e 0, ambos abaixo do mínimo.
    monkeypatch.setattr("app.main.gerar_embeddings", lambda textos, prefixo: [[-1.0, 0.0]])
    monkeypatch.setattr("app.main.httpx.post", lambda *a, **k: pytest.fail("não deveria chamar o modelo"))

    response = client.post("/perguntar", json={"pergunta": "Qual a capital da França?"})

    assert response.json() == {"resposta": MENSAGEM_SEM_RESPOSTA, "fontes": [], "truncada": False}


def test_perguntar_sem_documentos_indexados_nao_chama_ollama_nenhum(monkeypatch):
    monkeypatch.setattr("app.main.armazenamento", ArmazenamentoTrechos())
    monkeypatch.setattr("app.main.gerar_embeddings", lambda *a, **k: pytest.fail("sem embedding"))
    monkeypatch.setattr("app.main.httpx.post", lambda *a, **k: pytest.fail("sem modelo"))

    response = client.post("/perguntar", json={"pergunta": "oi"})

    assert response.json()["resposta"] == MENSAGEM_SEM_RESPOSTA


def test_perguntar_respeita_o_score_minimo_configurado(monkeypatch, armazenamento_com_rag_e_gatos):
    # Score do RAG vira 0,6: acima do mínimo padrão, mas abaixo de um mínimo mais rígido.
    monkeypatch.setattr("app.main.gerar_embeddings", lambda textos, prefixo: [[0.6, 0.8]])
    monkeypatch.setattr("app.main.config.RAG_SCORE_MINIMO", 0.9)
    monkeypatch.setattr("app.main.httpx.post", lambda *a, **k: pytest.fail("não deveria chamar"))

    response = client.post("/perguntar", json={"pergunta": "qualquer"})

    assert response.json()["fontes"] == []


def test_perguntar_avisa_quando_a_resposta_foi_truncada(monkeypatch, armazenamento_com_rag_e_gatos):
    monkeypatch.setattr("app.main.gerar_embeddings", lambda textos, prefixo: [[1.0, 0.0]])
    _chat_que_responde(monkeypatch, "cortada", done_reason="length")

    assert client.post("/perguntar", json={"pergunta": "x"}).json()["truncada"] is True


@pytest.mark.parametrize(
    "onde, erro, status",
    [
        ("embedding", httpx.ConnectError("recusada", request=REQ), 503),
        ("modelo", httpx.ReadTimeout("lento", request=REQ), 504),
    ],
)
def test_perguntar_traduz_falhas_do_ollama(monkeypatch, armazenamento_com_rag_e_gatos, onde, erro, status):
    def levanta(*a, **k):
        raise erro

    if onde == "embedding":
        monkeypatch.setattr("app.main.gerar_embeddings", levanta)
    else:
        monkeypatch.setattr("app.main.gerar_embeddings", lambda textos, prefixo: [[1.0, 0.0]])
        monkeypatch.setattr("app.main.httpx.post", levanta)

    assert client.post("/perguntar", json={"pergunta": "x"}).status_code == status


@pytest.mark.parametrize("corpo", [{"pergunta": ""}, {"pergunta": "x", "k": 0}, {"pergunta": "x", "k": 11}])
def test_perguntar_rejeita_pergunta_vazia_e_k_fora_do_limite(corpo):
    assert client.post("/perguntar", json=corpo).status_code == 422
