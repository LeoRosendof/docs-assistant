package com.docsassistant.backend.documento;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

// Sobe a aplicação inteira (controller, service, JPA, H2 em memória): o que se testa é o
// contrato HTTP de verdade. Cada teste é desfeito ao final por @Transactional.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	private int criar(String nome, String texto) throws Exception {
		String corpo = mockMvc.perform(post("/api/documentos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"" + nome + "\",\"texto\":\"" + texto + "\"}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(corpo, "$.id");
	}

	@Test
	void criarDevolve201ComLocationETextoSalvo() throws Exception {
		mockMvc.perform(post("/api/documentos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"manual.txt\",\"texto\":\"conteúdo do manual\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/documentos/")))
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.nome").value("manual.txt"))
				.andExpect(jsonPath("$.texto").value("conteúdo do manual"))
				.andExpect(jsonPath("$.caracteres").value(18))
				.andExpect(jsonPath("$.criadoEm").exists());
	}

	@Test
	void criarComNomeEmBrancoDevolve400() throws Exception {
		mockMvc.perform(post("/api/documentos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"   \",\"texto\":\"algo\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void criarSemTextoDevolve400() throws Exception {
		mockMvc.perform(post("/api/documentos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"a\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void criarComNomeMaiorQue200CaracteresDevolve400() throws Exception {
		mockMvc.perform(post("/api/documentos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"" + "x".repeat(201) + "\",\"texto\":\"algo\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void listarPaginaOsResultadosESemDevolverOTexto() throws Exception {
		criar("a", "texto a");
		criar("b", "texto b");
		criar("c", "texto c");

		mockMvc.perform(get("/api/documentos?page=0&size=2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.conteudo.length()").value(2))
				.andExpect(jsonPath("$.pagina").value(0))
				.andExpect(jsonPath("$.tamanho").value(2))
				.andExpect(jsonPath("$.totalElementos").value(3))
				.andExpect(jsonPath("$.totalPaginas").value(2))
				.andExpect(jsonPath("$.conteudo[0].texto").doesNotExist())
				.andExpect(jsonPath("$.conteudo[0].caracteres").isNumber());

		mockMvc.perform(get("/api/documentos?page=1&size=2"))
				.andExpect(jsonPath("$.conteudo.length()").value(1))
				.andExpect(jsonPath("$.pagina").value(1));
	}

	@Test
	void listarLimitaOTamanhoDaPaginaA50() throws Exception {
		mockMvc.perform(get("/api/documentos?size=1000"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tamanho").value(50));
	}

	@Test
	void buscarPorIdDevolveOTextoCompleto() throws Exception {
		int id = criar("guia", "texto completo do guia");

		mockMvc.perform(get("/api/documentos/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("guia"))
				.andExpect(jsonPath("$.texto").value("texto completo do guia"));
	}

	@Test
	void buscarInexistenteDevolve404() throws Exception {
		mockMvc.perform(get("/api/documentos/999999"))
				.andExpect(status().isNotFound());
	}

	@Test
	void removerDevolve204EODocumentoDeixaDeExistir() throws Exception {
		int id = criar("temporario", "vai sumir");

		mockMvc.perform(delete("/api/documentos/" + id)).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/documentos/" + id)).andExpect(status().isNotFound());
	}

	@Test
	void removerInexistenteDevolve404() throws Exception {
		mockMvc.perform(delete("/api/documentos/999999")).andExpect(status().isNotFound());
	}

}
