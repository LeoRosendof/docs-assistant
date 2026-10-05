def dividir_em_trechos(texto: str, tamanho: int = 500, sobreposicao: int = 100) -> list[str]:
    """Divide o texto em trechos de até `tamanho` caracteres.

    Trechos vizinhos compartilham `sobreposicao` caracteres, para que uma ideia
    cortada no limite de um trecho apareça inteira no seguinte. O corte é
    recuado até o último espaço, para não partir palavras ao meio.
    """
    if sobreposicao >= tamanho:
        raise ValueError("a sobreposição deve ser menor que o tamanho do trecho")

    texto = " ".join(texto.split())  # normaliza espaços e quebras de linha
    trechos = []
    inicio = 0
    while inicio < len(texto):
        fim = min(inicio + tamanho, len(texto))
        if fim < len(texto):
            ultimo_espaco = texto.rfind(" ", inicio, fim)
            # Só recua se isso ainda deixar o trecho avançando além da sobreposição.
            if ultimo_espaco > inicio + sobreposicao:
                fim = ultimo_espaco
        trechos.append(texto[inicio:fim].strip())
        if fim == len(texto):
            break
        proximo_inicio = fim - sobreposicao
        # O início do próximo trecho também não pode cair no meio de uma palavra:
        # avança até o espaço seguinte (a sobreposição fica um pouco menor).
        if texto[proximo_inicio - 1] != " " and texto[proximo_inicio] != " ":
            espaco = texto.find(" ", proximo_inicio, fim + 1)
            if espaco != -1:
                proximo_inicio = espaco + 1
        inicio = proximo_inicio
    return trechos
