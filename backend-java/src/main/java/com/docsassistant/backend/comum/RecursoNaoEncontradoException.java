package com.docsassistant.backend.comum;

/** Lançada quando um recurso pedido pelo cliente não existe; vira um 404 padronizado. */
public class RecursoNaoEncontradoException extends RuntimeException {

	public RecursoNaoEncontradoException(String mensagem) {
		super(mensagem);
	}

}
