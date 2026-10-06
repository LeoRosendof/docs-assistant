package com.docsassistant.backend.documento;

import java.time.Instant;

/** Versão da listagem: sem o texto, que pode ter centenas de milhares de caracteres. */
public record DocumentoResumoResponse(Long id, String nome, int caracteres, Instant criadoEm) {

	static DocumentoResumoResponse de(Documento d) {
		return new DocumentoResumoResponse(d.getId(), d.getNome(), d.getCaracteres(), d.getCriadoEm());
	}

}
