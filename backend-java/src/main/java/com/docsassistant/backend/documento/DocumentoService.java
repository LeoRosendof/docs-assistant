package com.docsassistant.backend.documento;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.docsassistant.backend.comum.PaginaResponse;

@Service
public class DocumentoService {

	private final DocumentoRepository repository;

	public DocumentoService(DocumentoRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public DocumentoResponse criar(DocumentoRequest request) {
		return DocumentoResponse.de(repository.save(new Documento(request.nome(), request.texto())));
	}

	@Transactional(readOnly = true)
	public PaginaResponse<DocumentoResumoResponse> listar(Pageable pageable) {
		return PaginaResponse.de(repository.findAll(pageable), DocumentoResumoResponse::de);
	}

	@Transactional(readOnly = true)
	public DocumentoResponse buscar(Long id) {
		return DocumentoResponse.de(obter(id));
	}

	@Transactional
	public void remover(Long id) {
		repository.delete(obter(id));
	}

	private Documento obter(Long id) {
		return repository.findById(id).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento " + id + " não encontrado."));
	}

}
