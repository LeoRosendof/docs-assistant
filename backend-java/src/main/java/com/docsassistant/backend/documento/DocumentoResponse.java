package com.docsassistant.backend.documento;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

public record DocumentoResponse(Long id, String nome, String texto, int caracteres, @Schema(description = "INDEXADO: pode responder perguntas. FALHOU: texto salvo, mas o serviço de IA falhou. PENDENTE: indexação em andamento.") StatusIndexacao status,
		@Schema(description = "Em quantos trechos o documento foi dividido; ausente se não foi indexado") Integer trechos, Instant criadoEm) {

	static DocumentoResponse de(Documento d) {
		return new DocumentoResponse(d.getId(), d.getNome(), d.getTexto(), d.getCaracteres(), d.getStatus(),
				d.getTrechos(), d.getCriadoEm());
	}

}
