package com.docsassistant.backend;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Sistema", description = "Verificações de funcionamento da API.")
@RestController
@RequestMapping("/api")
public class HealthController {

	@Operation(summary = "Verifica se a API está no ar")
	@GetMapping("/health")
	public Map<String, String> health() {
		return Map.of("status", "ok");
	}

}
