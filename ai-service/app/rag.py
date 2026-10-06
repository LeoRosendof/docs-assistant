from app.store import Trecho

MENSAGEM_SEM_RESPOSTA = "Não encontrei essa informação nos documentos indexados."

INSTRUCOES = f"""Você responde perguntas usando SOMENTE os trechos de documentos fornecidos como contexto.

Regras:
1. Se a resposta não estiver nos trechos, responda exatamente: "{MENSAGEM_SEM_RESPOSTA}"
2. Não use conhecimento externo e não invente informações.
3. Responda em português, de forma direta.
4. Indique os trechos usados no formato [1], [2].
5. Se os trechos cobrirem só parte da pergunta, diga o que consegue confirmar e o que não consegue."""


def montar_mensagens(pergunta: str, trechos: list[Trecho]) -> list[dict]:
    """Monta as mensagens do chat: instruções, trechos numerados e a pergunta.

    A numeração permite ao modelo citar a fonte e ao cliente conferir a resposta.
    """
    contexto = "\n\n".join(
        f"[{i}] ({t.documento_nome}, trecho {t.posicao})\n{t.texto}"
        for i, t in enumerate(trechos, start=1)
    )
    return [
        {"role": "system", "content": INSTRUCOES},
        {"role": "user", "content": f"Contexto:\n{contexto}\n\nPergunta: {pergunta}"},
    ]
