import math

import httpx
import pytest
from fastapi.testclient import TestClient

from app.main import app
from app.search import buscar_trechos, similaridade_cosseno
from app.store import ArmazenamentoTrechos, Trecho

client = TestClient(app)
REQ = httpx.Request("POST", "http://ollama/api/embed")


def test_cosseno_de_vetores_iguais_ortogonais_e_opostos():
    assert similaridade_cosseno([1, 0], [1, 0]) == pytest.approx(1.0)
    assert similaridade_cosseno([1, 0], [0, 1]) == pytest.approx(0.0)
    assert similaridade_cosseno([1, 0], [-1, 0]) == pytest.approx(-1.0)


def test_cosseno_ignora_o_tamanho_do_vetor_e_so_olha_a_direcao():
    assert similaridade_cosseno([1, 1], [5, 5]) == pytest.approx(1.0)
    assert similaridade_cosseno([3, 4], [0.6, 0.8]) == pytest.approx(1.0)


def test_cosseno_com_vetor_zero_devolve_zero_em_vez_de_dividir_por_zero():
    assert similaridade_cosseno([0, 0], [1, 2]) == 0.0


def _trecho(texto, vetor):
    return Trecho("doc1", "doc.txt", 0, texto, vetor)


def test_buscar_trechos_ordena_do_mais_parecido_ao_menos_e_respeita_k():
    angulo = lambda graus: [math.cos(math.radians(graus)), math.sin(math.radians(graus))]
    trechos = [_trecho("longe", angulo(80)), _trecho("perto", angulo(5)), _trecho("meio", angulo(40))]

    resultado = buscar_trechos([1.0, 0.0], trechos, k=2)

    assert [t.texto for t, _ in resultado] == ["perto", "meio"]
    assert resultado[0][1] > resultado[1][1]


@pytest.fixture
def armazenamento_com_dois_trechos(monkeypatch):
    armazenamento = ArmazenamentoTrechos()
    armazenamento.adicionar("d1", "gatos.txt", ["Gatos dormem muito.", "Gatos caçam à noite."], [[0.0, 1.0], [0.1, 0.9]])
    armazenamento.adicionar("d2", "rag.txt", ["RAG combina busca e geração."], [[1.0, 0.0]])
    monkeypatch.setattr("app.main.armazenamento", armazenamento)


def test_buscar_devolve_o_trecho_mais_proximo_com_documento_posicao_e_score(
    monkeypatch, armazenamento_com_dois_trechos
):
    monkeypatch.setattr("app.main.config.EMBED_PREFIXO_PERGUNTA", "search_query: ")
    usado = {}

    def falso_gerar(textos, prefixo):
        usado.update(textos=textos, prefixo=prefixo)
        return [[1.0, 0.0]]

    monkeypatch.setattr("app.main.gerar_embeddings", falso_gerar)

    response = client.post("/buscar", json={"pergunta": "O que é RAG?", "k": 1})

    assert response.status_code == 200
    assert response.json()["resultados"] == [
        {"documento": "rag.txt", "posicao": 0, "texto": "RAG combina busca e geração.", "score": 1.0}
    ]
    assert usado == {"textos": ["O que é RAG?"], "prefixo": "search_query: "}


def test_buscar_usa_k_padrao_de_tres(monkeypatch, armazenamento_com_dois_trechos):
    monkeypatch.setattr("app.main.gerar_embeddings", lambda textos, prefixo: [[1.0, 0.0]])

    response = client.post("/buscar", json={"pergunta": "qualquer"})

    assert len(response.json()["resultados"]) == 3


def test_buscar_sem_documentos_devolve_lista_vazia_sem_chamar_o_ollama(monkeypatch):
    monkeypatch.setattr("app.main.armazenamento", ArmazenamentoTrechos())
    monkeypatch.setattr("app.main.gerar_embeddings", lambda *a, **k: pytest.fail("não deveria chamar"))

    response = client.post("/buscar", json={"pergunta": "oi"})

    assert response.json() == {"resultados": []}


@pytest.mark.parametrize("corpo", [{"pergunta": ""}, {"pergunta": "oi", "k": 0}, {"pergunta": "oi", "k": 11}])
def test_buscar_rejeita_pergunta_vazia_e_k_fora_do_limite(corpo):
    assert client.post("/buscar", json=corpo).status_code == 422


def test_buscar_devolve_503_se_o_ollama_cair(monkeypatch, armazenamento_com_dois_trechos):
    def falha(textos, prefixo):
        raise httpx.ConnectError("recusada", request=REQ)

    monkeypatch.setattr("app.main.gerar_embeddings", falha)

    assert client.post("/buscar", json={"pergunta": "oi"}).status_code == 503
