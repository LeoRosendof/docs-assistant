package com.docsassistant.backend.documento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentoRequest(
		@NotBlank @Size(max = 200) String nome,
		@NotBlank @Size(max = 500_000) String texto) {
}
