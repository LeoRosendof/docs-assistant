package com.docsassistant.backend.comum;

import org.springframework.http.HttpStatus;

/** Falha ao falar com o ai-service; carrega o status HTTP que o cliente da API deve ver. */
public class ServicoIaException extends RuntimeException {

	private final HttpStatus status;

	public ServicoIaException(HttpStatus status, String mensagem) {
		super(mensagem);
		this.status = status;
	}

	public HttpStatus getStatus() {
		return status;
	}

}
