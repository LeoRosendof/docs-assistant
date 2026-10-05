from dataclasses import dataclass, field


@dataclass
class Trecho:
    documento_id: str
    documento_nome: str
    posicao: int
    texto: str
    vetor: list[float]


@dataclass
class ArmazenamentoTrechos:
    """Guarda os trechos e seus vetores em memória.

    Decisão consciente para esta etapa: é o suficiente para provar o RAG e não
    exige banco nem dependência extra. Os dados somem ao reiniciar o serviço; os
    documentos "de verdade" ficarão no Spring Boot, e o ai-service só os indexa.
    """

    trechos: list[Trecho] = field(default_factory=list)

    def adicionar(self, documento_id: str, nome: str, textos: list[str], vetores: list[list[float]]):
        for posicao, (texto, vetor) in enumerate(zip(textos, vetores, strict=True)):
            self.trechos.append(Trecho(documento_id, nome, posicao, texto, vetor))

    def resumo_por_documento(self) -> list[dict]:
        documentos: dict[str, dict] = {}
        for t in self.trechos:
            d = documentos.setdefault(
                t.documento_id, {"id": t.documento_id, "nome": t.documento_nome, "trechos": 0}
            )
            d["trechos"] += 1
        return list(documentos.values())


armazenamento = ArmazenamentoTrechos()
