package com.docsassistant.backend.documento;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.docsassistant.backend.comum.PaginaResponse;
import com.docsassistant.backend.comum.RecursoNaoEncontradoException;
import com.docsassistant.backend.comum.ServicoIaException;
import com.docsassistant.backend.ia.AiServiceClient;

/**
 * Os métodos que chamam o ai-service não são @Transactional de propósito: cada
 * chamada ao repositório já é uma transação curta, e assim nenhuma fica aberta
 * enquanto esperamos a rede (a indexação pode levar vários segundos).
 */
@Service
public class DocumentoService {

	private static final Logger log = LoggerFactory.getLogger(DocumentoService.class);

	private final DocumentoRepository repository;
	private final AiServiceClient aiService;

	public DocumentoService(DocumentoRepository repository, AiServiceClient aiService) {
		this.repository = repository;
		this.aiService = aiService;
	}

	/** Salva e tenta indexar. Se a indexação falhar, o documento fica salvo com status FALHOU. */
	public DocumentoResponse criar(DocumentoRequest request) {
		Documento documento = repository.save(new Documento(request.nome(), request.texto()));
		try {
			indexar(documento);
		}
		catch (ServicoIaException e) {
			log.warn("Documento {} salvo, mas a indexação falhou: {}", documento.getId(), e.getMessage());
		}
		return DocumentoResponse.de(documento);
	}

	/** Nova tentativa de indexação. Aqui a falha é devolvida ao cliente, que pediu a operação. */
	public DocumentoResponse reindexar(Long id) {
		Documento documento = obter(id);
		indexar(documento);
		return DocumentoResponse.de(documento);
	}

	@Transactional(readOnly = true)
	public PaginaResponse<DocumentoResumoResponse> listar(Pageable pageable) {
		return PaginaResponse.de(repository.findAll(pageable), DocumentoResumoResponse::de);
	}

	@Transactional(readOnly = true)
	public DocumentoResponse buscar(Long id) {
		return DocumentoResponse.de(obter(id));
	}

	/** Apaga no banco e, se possível, do índice. O índice fora do ar não impede a remoção. */
	public void remover(Long id) {
		Documento documento = obter(id);
		repository.delete(documento);
		try {
			aiService.remover(String.valueOf(id));
		}
		catch (ServicoIaException e) {
			log.warn("Documento {} apagado, mas ficou no índice do ai-service: {}", id, e.getMessage());
		}
	}

	private void indexar(Documento documento) {
		try {
			int trechos = aiService.indexar(String.valueOf(documento.getId()), documento.getNome(), documento.getTexto());
			documento.marcarIndexado(trechos);
		}
		catch (ServicoIaException e) {
			documento.marcarFalha();
			repository.save(documento);
			throw e;
		}
		repository.save(documento);
	}

	private Documento obter(Long id) {
		return repository.findById(id).orElseThrow(
				() -> new RecursoNaoEncontradoException("Documento " + id + " não encontrado."));
	}

}
