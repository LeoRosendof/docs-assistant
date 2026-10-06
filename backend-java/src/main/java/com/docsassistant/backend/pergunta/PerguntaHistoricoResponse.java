package com.docsassistant.backend.pergunta;

import java.time.Instant;

public record PerguntaHistoricoResponse(Long id, String pergunta, String resposta, boolean truncada, Instant criadoEm) {

	static PerguntaHistoricoResponse de(Pergunta p) {
		return new PerguntaHistoricoResponse(p.getId(), p.getPergunta(), p.getResposta(), p.isTruncada(), p.getCriadoEm());
	}

}
