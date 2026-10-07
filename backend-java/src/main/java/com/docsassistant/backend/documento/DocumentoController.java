package com.docsassistant.backend.documento;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
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
import com.docsassistant.backend.comum.UsaServicoIa;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Documentos", description = "Textos que o assistente usa para responder; cada um é indexado no serviço de IA.")
@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

	private final DocumentoService service;

	public DocumentoController(DocumentoService service) {
		this.service = service;
	}

	@Operation(summary = "Cria um documento e o indexa",
			description = "Salva o texto e tenta indexá-lo. Se o serviço de IA falhar, o documento fica salvo "
					+ "com status `FALHOU` (resposta 201) e dá para tentar de novo em `/{id}/indexar`.")
	@ApiResponse(responseCode = "201", description = "Documento criado; veja `status` para saber se foi indexado.")
	@PostMapping
	public ResponseEntity<DocumentoResponse> criar(@Valid @RequestBody DocumentoRequest request) {
		DocumentoResponse criado = service.criar(request);
		return ResponseEntity.created(URI.create("/api/documentos/" + criado.id())).body(criado);
	}

	@Operation(summary = "Lista os documentos, do mais recente ao mais antigo",
			description = "Paginada (`page` começa em 0; `size` máximo de 50). Não traz o texto, só um resumo.")
	@GetMapping
	public PaginaResponse<DocumentoResumoResponse> listar(
			@ParameterObject @PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
		return service.listar(pageable);
	}

	@Operation(summary = "Busca um documento pelo id, com o texto completo")
	@GetMapping("/{id}")
	public DocumentoResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

	@Operation(summary = "Tenta indexar o documento de novo",
			description = "Útil depois de uma falha, ou se o serviço de IA foi reiniciado e perdeu o índice.")
	@UsaServicoIa
	@PostMapping("/{id}/indexar")
	public DocumentoResponse reindexar(@PathVariable Long id) {
		return service.reindexar(id);
	}

	@Operation(summary = "Apaga o documento e o tira do índice",
			description = "Se o serviço de IA estiver fora do ar, o documento é apagado mesmo assim.")
	@ApiResponse(responseCode = "204", description = "Documento apagado.")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remover(@PathVariable Long id) {
		service.remover(id);
		return ResponseEntity.noContent().build();
	}

}
