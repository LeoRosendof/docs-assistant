package com.docsassistant.backend.comum;

import java.util.Arrays;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

/**
 * Documentação da API. Os erros são acrescentados aqui, de forma geral, em vez de repetir
 * as mesmas anotações em cada rota: o handler global já garante que todo erro sai no mesmo
 * formato, e a documentação reflete isso.
 */
@Configuration
class OpenApiConfig {

	private static final String PROBLEM_JSON = "application/problem+json";

	@Bean
	OpenAPI docsAssistantOpenApi() {
		return new OpenAPI().info(new Info()
				.title("docs-assistant API")
				.version("0.1.0")
				.description("""
						API REST do assistente de perguntas sobre documentos (RAG).

						Os documentos são salvos aqui e indexados no serviço de IA; as perguntas são
						respondidas com base nos trechos mais parecidos, citando as fontes.
						Todos os erros seguem o formato `application/problem+json` (RFC 7807)."""));
	}

	/** Registra o schema do corpo de erro, que não aparece em nenhuma assinatura de método. */
	@Bean
	OpenApiCustomizer schemaDeErro() {
		return openApi -> {
			ResolvedSchema resolvido = ModelConverters.getInstance()
					.resolveAsResolvedSchema(new AnnotatedType(Problema.class));
			openApi.getComponents().addSchemas("Problema", resolvido.schema);
			if (resolvido.referencedSchemas != null) {
				resolvido.referencedSchemas.forEach(openApi.getComponents()::addSchemas);
			}
		};
	}

	@Bean
	OperationCustomizer respostasDeErro() {
		return (operation, handlerMethod) -> {
			boolean temId = Arrays.stream(handlerMethod.getMethodParameters())
					.anyMatch(p -> p.hasParameterAnnotation(PathVariable.class));
			boolean temCorpo = Arrays.stream(handlerMethod.getMethodParameters())
					.anyMatch(p -> p.hasParameterAnnotation(RequestBody.class));

			if (temCorpo || temId) {
				erro(operation, "400", temCorpo
						? "Requisição inválida: campos inválidos (lista em `erros`) ou JSON malformado."
						: "Requisição inválida: o id não é um número.");
			}
			if (temId) {
				erro(operation, "404", "Recurso não encontrado.");
			}
			if (handlerMethod.hasMethodAnnotation(UsaServicoIa.class)) {
				erro(operation, "502", "O serviço de IA respondeu com erro.");
				erro(operation, "503", "O serviço de IA está fora do ar.");
				erro(operation, "504", "O serviço de IA demorou demais para responder.");
			}
			erro(operation, "500", "Erro interno inesperado (sem detalhes internos).");
			return operation;
		};
	}

	private static void erro(Operation operation, String codigo, String descricao) {
		Schema<?> referencia = new Schema<>().$ref("#/components/schemas/Problema");
		operation.getResponses().addApiResponse(codigo, new ApiResponse().description(descricao)
				.content(new Content().addMediaType(PROBLEM_JSON, new MediaType().schema(referencia))));
	}

}
