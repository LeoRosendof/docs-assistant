import httpx

from app import config

# O nomic-embed-text foi treinado com prefixos que distinguem o papel do texto:
# trechos guardados e perguntas usam prefixos diferentes para ficarem comparáveis.
PREFIXO_DOCUMENTO = "search_document: "
PREFIXO_PERGUNTA = "search_query: "


def gerar_embeddings(textos: list[str], prefixo: str = PREFIXO_DOCUMENTO) -> list[list[float]]:
    """Gera um vetor por texto, numa única chamada ao Ollama.

    Falhas de rede propagam como httpx.HTTPError; quem chama decide o status HTTP.
    """
    resposta = httpx.post(
        config.OLLAMA_EMBED_URL,
        json={"model": config.OLLAMA_EMBED_MODELO, "input": [prefixo + t for t in textos]},
        timeout=config.OLLAMA_TIMEOUT,
    )
    resposta.raise_for_status()
    return resposta.json()["embeddings"]
