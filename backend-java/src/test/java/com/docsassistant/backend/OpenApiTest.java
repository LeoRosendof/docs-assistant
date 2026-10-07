package com.docsassistant.backend;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * A especificação OpenAPI é o contrato que o frontend lê, então ela é testada como código:
 * o que ela promete tem que ser o que a API faz (status reais, erros padronizados, paginação).
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiTest {

	private static final String PROBLEM = "application/problem+json";

	@Autowired
	private MockMvc mockMvc;

	private ResultActions especificacao() throws Exception {
		return mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}

	@Test
	void descreveAApiComTituloEDescricao() throws Exception {
		especificacao()
				.andExpect(jsonPath("$.info.title").value("docs-assistant API"))
				.andExpect(jsonPath("$.info.version").value("0.1.0"))
				.andExpect(jsonPath("$.info.description").value(org.hamcrest.Matchers.containsString("RFC 7807")))
				.andExpect(jsonPath("$.tags[*].name", hasItems("Documentos", "Perguntas", "Sistema")));
	}

	@Test
	void documentaOStatusRealDeCadaOperacaoEnaoUmGenerico200() throws Exception {
		especificacao()
				.andExpect(jsonPath("$.paths['/api/documentos'].post.responses['201']").exists())
				.andExpect(jsonPath("$.paths['/api/documentos'].post.responses['200']").doesNotExist())
				.andExpect(jsonPath("$.paths['/api/perguntas'].post.responses['201']").exists())
				.andExpect(jsonPath("$.paths['/api/perguntas'].post.responses['200']").doesNotExist())
				.andExpect(jsonPath("$.paths['/api/documentos/{id}'].delete.responses['204']").exists())
				.andExpect(jsonPath("$.paths['/api/documentos/{id}'].delete.responses['200']").doesNotExist())
				.andExpect(jsonPath("$.paths['/api/documentos'].get.responses['200']").exists());
	}

	@Test
	void documentaOsErrosEmProblemJsonComOMesmoSchemaDoHandler() throws Exception {
		especificacao()
				.andExpect(jsonPath("$.paths['/api/documentos/{id}'].get.responses['404'].content['" + PROBLEM
						+ "'].schema['$ref']").value("#/components/schemas/Problema"))
				.andExpect(jsonPath("$.paths['/api/documentos'].post.responses['400']").exists())
				.andExpect(jsonPath("$.paths['/api/documentos'].post.responses['500']").exists())
				.andExpect(jsonPath("$.components.schemas.Problema.properties.keys()",
						containsInAnyOrder("title", "status", "detail", "instance", "erros")))
				.andExpect(jsonPath("$.components.schemas.ErroDeCampo.properties.keys()",
						containsInAnyOrder("campo", "mensagem")));
	}

	@Test
	void soAsRotasQueDependemDaIaDocumentamErrosDeGateway() throws Exception {
		especificacao()
				.andExpect(jsonPath("$.paths['/api/perguntas'].post.responses.keys()", hasItems("502", "503", "504")))
				.andExpect(jsonPath("$.paths['/api/documentos/{id}/indexar'].post.responses.keys()",
						hasItems("502", "503", "504", "404")))
				// criar e listar não falham por causa da IA: criar guarda o documento mesmo assim
				.andExpect(jsonPath("$.paths['/api/documentos'].post.responses.keys()", not(hasItems("503"))))
				.andExpect(jsonPath("$.paths['/api/documentos'].get.responses.keys()", not(hasItems("503", "404"))));
	}

	@Test
	void paginacaoApareceComoPageSizeESortENaoComoUmObjetoPageable() throws Exception {
		especificacao()
				.andExpect(jsonPath("$.paths['/api/documentos'].get.parameters[*].name",
						hasItems("page", "size", "sort")))
				.andExpect(jsonPath("$.paths['/api/documentos'].get.parameters[*].name", not(hasItems("pageable"))))
				.andExpect(jsonPath("$.paths['/api/perguntas'].get.parameters[*].name",
						hasItems("page", "size", "sort")));
	}

	@Test
	void requestsTemCamposObrigatoriosLimitesEExemplos() throws Exception {
		especificacao()
				.andExpect(jsonPath("$.components.schemas.DocumentoRequest.required",
						containsInAnyOrder("nome", "texto")))
				.andExpect(jsonPath("$.components.schemas.DocumentoRequest.properties.nome.maxLength").value(200))
				.andExpect(jsonPath("$.components.schemas.DocumentoRequest.properties.nome.example").value("manual.txt"))
				.andExpect(jsonPath("$.components.schemas.PerguntaRequest.required", hasItems("pergunta")))
				.andExpect(jsonPath("$.components.schemas.PerguntaRequest.properties.pergunta.example").exists());
	}

	@Test
	void cadaOperacaoTemUmResumo() throws Exception {
		especificacao()
				.andExpect(jsonPath("$.paths[*][*].summary").exists())
				.andExpect(jsonPath("$.paths['/api/documentos'].post.summary").value("Cria um documento e o indexa"))
				.andExpect(jsonPath("$.paths['/api/perguntas'].post.summary")
						.value("Faz uma pergunta aos documentos indexados"));
	}

	@Test
	void interfaceDoSwaggerEstaDisponivel() throws Exception {
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
	}

}
