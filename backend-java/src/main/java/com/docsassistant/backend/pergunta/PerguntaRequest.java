package com.docsassistant.backend.pergunta;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerguntaRequest(
		@NotBlank(message = "é obrigatória") @Size(max = 2000, message = "deve ter no máximo 2000 caracteres") String pergunta) {
}
