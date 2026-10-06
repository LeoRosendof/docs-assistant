package com.docsassistant.backend.pergunta;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docsassistant.backend.comum.PaginaResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/perguntas")
public class PerguntaController {

	private final PerguntaService service;

	public PerguntaController(PerguntaService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<PerguntaResponse> perguntar(@Valid @RequestBody PerguntaRequest request) {
		PerguntaResponse resposta = service.perguntar(request.pergunta());
		return ResponseEntity.created(URI.create("/api/perguntas/" + resposta.id())).body(resposta);
	}

	@GetMapping
	public PaginaResponse<PerguntaHistoricoResponse> historico(
			@PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
		return service.historico(pageable);
	}

	@GetMapping("/{id}")
	public PerguntaHistoricoResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

}
