package com.docsassistant.backend.comum;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Contrato próprio de paginação. Devolver o Page do Spring direto amarraria o JSON
 * do frontend aos detalhes internos da biblioteca.
 */
public record PaginaResponse<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos, int totalPaginas) {

	public static <E, T> PaginaResponse<T> de(Page<E> page, Function<E, T> conversor) {
		return new PaginaResponse<>(
				page.getContent().stream().map(conversor).toList(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages());
	}

}
