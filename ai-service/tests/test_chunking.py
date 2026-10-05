import pytest

from app.chunking import dividir_em_trechos


def test_texto_curto_vira_um_unico_trecho():
    assert dividir_em_trechos("Um texto curto.") == ["Um texto curto."]


def test_texto_vazio_ou_so_espacos_nao_gera_trechos():
    assert dividir_em_trechos("") == []
    assert dividir_em_trechos("  \n\t ") == []


def test_normaliza_espacos_e_quebras_de_linha():
    assert dividir_em_trechos("linha um\n\nlinha   dois") == ["linha um linha dois"]


def test_nenhum_trecho_passa_do_tamanho_e_nenhuma_palavra_e_partida():
    palavras = [f"palavra{i}" for i in range(200)]
    trechos = dividir_em_trechos(" ".join(palavras), tamanho=100, sobreposicao=20)

    assert len(trechos) > 1
    assert all(len(t) <= 100 for t in trechos)
    conhecidas = set(palavras)
    assert all(p in conhecidas for t in trechos for p in t.split())


def test_trechos_vizinhos_compartilham_conteudo():
    palavras = [f"p{i}" for i in range(100)]
    trechos = dividir_em_trechos(" ".join(palavras), tamanho=80, sobreposicao=30)

    for anterior, seguinte in zip(trechos, trechos[1:]):
        assert set(anterior.split()) & set(seguinte.split())


def test_nao_perde_nenhuma_palavra():
    palavras = [f"p{i}" for i in range(300)]
    trechos = dividir_em_trechos(" ".join(palavras), tamanho=90, sobreposicao=25)

    vistas = {p for t in trechos for p in t.split()}
    assert vistas == set(palavras)


def test_palavra_maior_que_o_trecho_nao_causa_loop_infinito():
    trechos = dividir_em_trechos("x" * 1000, tamanho=100, sobreposicao=20)

    assert all(len(t) <= 100 for t in trechos)
    assert len(trechos) > 1


def test_sobreposicao_invalida_e_rejeitada():
    with pytest.raises(ValueError):
        dividir_em_trechos("texto", tamanho=50, sobreposicao=50)
