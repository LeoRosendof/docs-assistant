package com.docsassistant.backend.documento;

import java.time.Instant;

public record DocumentoResponse(Long id, String nome, String texto, int caracteres, StatusIndexacao status,
		Integer trechos, Instant criadoEm) {

	static DocumentoResponse de(Documento d) {
		return new DocumentoResponse(d.getId(), d.getNome(), d.getTexto(), d.getCaracteres(), d.getStatus(),
				d.getTrechos(), d.getCriadoEm());
	}

}
