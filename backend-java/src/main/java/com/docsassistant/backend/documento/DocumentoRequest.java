package com.docsassistant.backend.documento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentoRequest(
		@NotBlank(message = "é obrigatório") @Size(max = 200, message = "deve ter no máximo 200 caracteres") String nome,
		@NotBlank(message = "é obrigatório") @Size(max = 500_000, message = "deve ter no máximo 500000 caracteres") String texto) {
}
