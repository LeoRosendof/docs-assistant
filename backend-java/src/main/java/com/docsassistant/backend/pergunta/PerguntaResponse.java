package com.docsassistant.backend.pergunta;

import java.time.Instant;
import java.util.List;

/** Resposta de uma pergunta nova: traz as fontes, que não ficam guardadas no histórico. */
public record PerguntaResponse(Long id, String pergunta, String resposta, boolean truncada,
		List<FonteResponse> fontes, Instant criadoEm) {
}
