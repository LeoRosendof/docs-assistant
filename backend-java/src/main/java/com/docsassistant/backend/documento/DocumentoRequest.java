package com.docsassistant.backend.documento;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentoRequest(
		@Schema(description = "Nome do documento, só para identificá-lo", example = "manual.txt")
		@NotBlank(message = "é obrigatório") @Size(max = 200, message = "deve ter no máximo 200 caracteres") String nome,
		@Schema(description = "Conteúdo do documento, já em texto", example = "Cada trabalho deve ser lido por pelo menos 2 pesquisadores.")
		@NotBlank(message = "é obrigatório") @Size(max = 500_000, message = "deve ter no máximo 500000 caracteres") String texto) {
}
