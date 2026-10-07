package com.docsassistant.backend.pergunta;

import java.net.URI;

import org.springdoc.core.annotations.ParameterObject;
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
import com.docsassistant.backend.comum.UsaServicoIa;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Perguntas", description = "Perguntas ao assistente e o histórico de respostas.")
@RestController
@RequestMapping("/api/perguntas")
public class PerguntaController {

	private final PerguntaService service;

	public PerguntaController(PerguntaService service) {
		this.service = service;
	}

	@Operation(summary = "Faz uma pergunta aos documentos indexados",
			description = "Devolve a resposta e as fontes (trechos) em que ela se baseou. Se a informação não "
					+ "estiver nos documentos, a resposta diz que não encontrou. A pergunta fica no histórico; as fontes não. "
					+ "A primeira chamada pode levar ~30 s, enquanto o modelo carrega.")
	@ApiResponse(responseCode = "201", description = "Pergunta respondida e guardada no histórico.")
	@UsaServicoIa
	@PostMapping
	public ResponseEntity<PerguntaResponse> perguntar(@Valid @RequestBody PerguntaRequest request) {
		PerguntaResponse resposta = service.perguntar(request.pergunta());
		return ResponseEntity.created(URI.create("/api/perguntas/" + resposta.id())).body(resposta);
	}

	@Operation(summary = "Histórico de perguntas, da mais recente à mais antiga")
	@GetMapping
	public PaginaResponse<PerguntaHistoricoResponse> historico(
			@ParameterObject @PageableDefault(size = 10, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
		return service.historico(pageable);
	}

	@Operation(summary = "Busca uma pergunta do histórico pelo id")
	@GetMapping("/{id}")
	public PerguntaHistoricoResponse buscar(@PathVariable Long id) {
		return service.buscar(id);
	}

}
