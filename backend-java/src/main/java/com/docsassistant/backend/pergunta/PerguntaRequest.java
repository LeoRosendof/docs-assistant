package com.docsassistant.backend.pergunta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerguntaRequest(
		@Schema(description = "Pergunta em linguagem natural", example = "Quantos pesquisadores devem ler cada trabalho?")
		@NotBlank(message = "é obrigatória") @Size(max = 2000, message = "deve ter no máximo 2000 caracteres") String pergunta) {
}
