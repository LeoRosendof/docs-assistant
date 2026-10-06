package com.docsassistant.backend.documento;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docsassistant.backend.comum.PaginaResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

	private final DocumentoService service;

	public DocumentoController(DocumentoService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<DocumentoResponse> criar(@Valid @RequestBody DocumentoRequest request) {
		DocumentoResponse criado = service.criar(request);
		return ResponseEntity.created(URI.create("/api/documentos/" + criado.id())).body(criado);
	}

	@GetMapping
	public PaginaResponse<DocumentoResumoResponse> listar(
			@PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
		return service.listar(pageable);
	}

	@GetMapping("/{id}")
	public DocumentoResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remover(@PathVariable Long id) {
		service.remover(id);
		return ResponseEntity.noContent().build();
	}

}
