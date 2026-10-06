package com.docsassistant.backend.comum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ErrosPadronizadosTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void validacaoIndicaQualCampoFalhouEPorque() throws Exception {
		mockMvc.perform(post("/api/documentos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"\",\"texto\":\"conteúdo\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.title").value("Requisição inválida"))
				.andExpect(jsonPath("$.detail").value("Há campos inválidos na requisição."))
				.andExpect(jsonPath("$.instance").value("/api/documentos"))
				.andExpect(jsonPath("$.erros.length()").value(1))
				.andExpect(jsonPath("$.erros[0].campo").value("nome"))
				.andExpect(jsonPath("$.erros[0].mensagem").value("é obrigatório"));
	}

	@Test
	void validacaoListaTodosOsCamposInvalidosDeUmaVez() throws Exception {
		mockMvc.perform(post("/api/documentos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"\",\"texto\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[*].campo", containsInAnyOrder("nome", "texto")));
	}

	@Test
	void validacaoDeTamanhoTraMensagemEmPortugues() throws Exception {
		mockMvc.perform(post("/api/documentos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"" + "x".repeat(201) + "\",\"texto\":\"ok\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("nome"))
				.andExpect(jsonPath("$.erros[0].mensagem").value("deve ter no máximo 200 caracteres"));
	}

	@Test
	void jsonMalFormadoDevolve400Padronizado() throws Exception {
		mockMvc.perform(post("/api/documentos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{nome"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Requisição inválida"))
				.andExpect(jsonPath("$.detail").value("O corpo da requisição está ausente ou não é um JSON válido."));
	}

	@Test
	void corpoAusenteDevolve400Padronizado() throws Exception {
		mockMvc.perform(post("/api/documentos").contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.title").value("Requisição inválida"));
	}

	@Test
	void documentoInexistenteDevolve404ComMensagemUtil() throws Exception {
		mockMvc.perform(get("/api/documentos/999999"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.title").value("Recurso não encontrado"))
				.andExpect(jsonPath("$.detail").value("Documento 999999 não encontrado."))
				.andExpect(jsonPath("$.instance").value("/api/documentos/999999"));
	}

	@Test
	void idNaoNumericoDevolve400Padronizado() throws Exception {
		mockMvc.perform(get("/api/documentos/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Requisição inválida"))
				.andExpect(jsonPath("$.detail").value("O valor 'abc' não é válido para 'id'."));
	}

	@Test
	void rotaInexistenteDevolve404Padronizado() throws Exception {
		mockMvc.perform(get("/api/nao-existe"))
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Recurso não encontrado"));
	}

	@Test
	void metodoNaoPermitidoDevolve405Padronizado() throws Exception {
		mockMvc.perform(put("/api/documentos").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.title").value("Método não permitido"))
				.andExpect(jsonPath("$.status").value(405));
	}

	@Test
	void contentTypeNaoSuportadoDevolve415Padronizado() throws Exception {
		mockMvc.perform(post("/api/documentos").contentType(MediaType.TEXT_PLAIN).content("texto solto"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.title").value("Tipo de conteúdo não suportado"))
				.andExpect(jsonPath("$.detail", containsString("text/plain")));
	}

	@Test
	void erroInesperadoDevolve500SemVazarDetalhesInternos() {
		ProblemDetail problema = new GlobalExceptionHandler()
				.erroInesperado(new IllegalStateException("senha do banco: abc123"));

		assertThat(problema.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
		assertThat(problema.getTitle()).isEqualTo("Erro interno");
		assertThat(problema.getDetail()).doesNotContain("senha").doesNotContain("abc123");
	}

}
