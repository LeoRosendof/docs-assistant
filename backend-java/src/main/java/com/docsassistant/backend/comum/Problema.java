package com.docsassistant.backend.comum;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Apenas documentação: descreve no OpenAPI o corpo de erro (RFC 7807) que o
 * GlobalExceptionHandler devolve. Não é usado em tempo de execução.
 */
@Schema(description = "Erro padronizado (RFC 7807, application/problem+json)")
public record Problema(
		@Schema(description = "Resumo curto do tipo de erro", example = "Requisição inválida") String title,
		@Schema(description = "Código HTTP", example = "400") int status,
		@Schema(description = "Explicação do que deu errado", example = "Há campos inválidos na requisição.") String detail,
		@Schema(description = "Rota que originou o erro", example = "/api/documentos") String instance,
		@Schema(description = "Só aparece em erros de validação: um item por campo inválido") List<ErroDeCampo> erros) {

	@Schema(description = "Um campo inválido da requisição")
	public record ErroDeCampo(
			@Schema(example = "nome") String campo,
			@Schema(example = "é obrigatório") String mensagem) {
	}

}
