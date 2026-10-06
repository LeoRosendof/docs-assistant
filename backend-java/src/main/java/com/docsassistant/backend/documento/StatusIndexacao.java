package com.docsassistant.backend.documento;

/** Situação do documento no índice de busca do ai-service. */
public enum StatusIndexacao {
	/** Salvo, mas a indexação ainda não terminou. */
	PENDENTE,
	/** Indexado: pode ser usado para responder perguntas. */
	INDEXADO,
	/** O ai-service falhou; o texto está salvo e dá para tentar de novo. */
	FALHOU
}
