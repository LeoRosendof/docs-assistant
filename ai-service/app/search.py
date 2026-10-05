import math

from app.store import Trecho


def similaridade_cosseno(a: list[float], b: list[float]) -> float:
    """Cosseno entre dois vetores: 1 = mesma direção, 0 = sem relação.

    Os vetores do nomic-embed-text já vêm normalizados (norma 1), mas dividir
    pelas normas evita depender dessa promessa do provedor.
    """
    produto = sum(x * y for x, y in zip(a, b, strict=True))
    norma = math.sqrt(sum(x * x for x in a)) * math.sqrt(sum(y * y for y in b))
    return produto / norma if norma else 0.0


def buscar_trechos(
    vetor_pergunta: list[float], trechos: list[Trecho], k: int = 3
) -> list[tuple[Trecho, float]]:
    """Devolve os `k` trechos mais parecidos com a pergunta, do melhor para o pior."""
    pontuados = [(t, similaridade_cosseno(vetor_pergunta, t.vetor)) for t in trechos]
    pontuados.sort(key=lambda par: par[1], reverse=True)
    return pontuados[:k]
