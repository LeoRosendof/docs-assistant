package com.docsassistant.backend.pergunta;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.docsassistant.backend.comum.PaginaResponse;
import com.docsassistant.backend.comum.RecursoNaoEncontradoException;
import com.docsassistant.backend.ia.AiServiceClient;
import com.docsassistant.backend.ia.RespostaIa;

@Service
public class PerguntaService {

	private final PerguntaRepository repository;
	private final AiServiceClient aiService;

	public PerguntaService(PerguntaRepository repository, AiServiceClient aiService) {
		this.repository = repository;
		this.aiService = aiService;
	}

	/**
	 * Pergunta ao ai-service e só então guarda no histórico: se a IA falhar, nada é
	 * gravado. Não é @Transactional para não segurar uma transação durante a espera.
	 */
	public PerguntaResponse perguntar(String texto) {
		RespostaIa resposta = aiService.perguntar(texto);
		Pergunta salva = repository.save(new Pergunta(texto, resposta.resposta(), resposta.truncada()));
		return new PerguntaResponse(
				salva.getId(), salva.getPergunta(), salva.getResposta(), salva.isTruncada(),
				resposta.fontes().stream()
						.map(f -> new FonteResponse(f.documento(), f.posicao(), f.texto(), f.score()))
						.toList(),
				salva.getCriadoEm());
	}

	@Transactional(readOnly = true)
	public PaginaResponse<PerguntaHistoricoResponse> historico(Pageable pageable) {
		return PaginaResponse.de(repository.findAll(pageable), PerguntaHistoricoResponse::de);
	}

	@Transactional(readOnly = true)
	public PerguntaHistoricoResponse buscar(Long id) {
		return repository.findById(id).map(PerguntaHistoricoResponse::de).orElseThrow(
				() -> new RecursoNaoEncontradoException("Pergunta " + id + " não encontrada."));
	}

}
