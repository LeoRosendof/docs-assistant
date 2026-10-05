import httpx

from app import config


def gerar_embeddings(textos: list[str], prefixo: str | None = None) -> list[list[float]]:
    """Gera um vetor por texto, numa única chamada ao Ollama.

    `prefixo` é somado a cada texto; sem ele, usa o prefixo de documento da
    configuração. Falhas de rede propagam como httpx.HTTPError; quem chama
    decide o status HTTP.
    """
    if prefixo is None:
        prefixo = config.EMBED_PREFIXO_DOCUMENTO
    resposta = httpx.post(
        config.OLLAMA_EMBED_URL,
        json={"model": config.OLLAMA_EMBED_MODELO, "input": [prefixo + t for t in textos]},
        timeout=config.OLLAMA_TIMEOUT,
    )
    resposta.raise_for_status()
    return resposta.json()["embeddings"]
