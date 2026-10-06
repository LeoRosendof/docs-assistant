package com.docsassistant.backend.documento;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.docsassistant.backend.comum.ServicoIaException;
import com.docsassistant.backend.ia.AiServiceClient;
import com.jayway.jsonpath.JsonPath;

// Sobe a aplicação inteira (controller, service, JPA, H2 em memória): o que se testa é o
// contrato HTTP de verdade. Cada teste é desfeito ao final por @Transactional.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DocumentoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	// O ai-service é simulado: estes testes verificam o backend, não a rede nem o Ollama.
	@MockitoBean
	private AiServiceClient aiService;

	@BeforeEach
	void iaIndexaEmTresTrechos() {
		when(aiService.indexar(any(), any(), any())).thenReturn(3);
	}

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

	@Test
	void criarIndexaNoAiServiceEMarcaOStatus() throws Exception {
		mockMvc.perform(post("/api/documentos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"guia.txt\",\"texto\":\"conteúdo do guia\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("INDEXADO"))
				.andExpect(jsonPath("$.trechos").value(3));

		// O id enviado ao ai-service é o do banco, para poder reindexar e apagar depois.
		verify(aiService).indexar(any(String.class), eq("guia.txt"), eq("conteúdo do guia"));
	}

	@Test
	void criarGuardaODocumentoMesmoSeAIndexacaoFalhar() throws Exception {
		when(aiService.indexar(any(), any(), any()))
				.thenThrow(new ServicoIaException(HttpStatus.SERVICE_UNAVAILABLE, "fora do ar"));

		String corpo = mockMvc.perform(post("/api/documentos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nome\":\"importante.txt\",\"texto\":\"não pode se perder\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("FALHOU"))
				.andExpect(jsonPath("$.trechos").doesNotExist())
				.andReturn().getResponse().getContentAsString();
		int id = JsonPath.read(corpo, "$.id");

		mockMvc.perform(get("/api/documentos/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("FALHOU"))
				.andExpect(jsonPath("$.texto").value("não pode se perder"));
	}

	@Test
	void reindexarDepoisDeUmaFalhaMarcaComoIndexado() throws Exception {
		when(aiService.indexar(any(), any(), any()))
				.thenThrow(new ServicoIaException(HttpStatus.SERVICE_UNAVAILABLE, "fora do ar"))
				.thenReturn(5);
		int id = criar("a", "texto a");

		mockMvc.perform(post("/api/documentos/" + id + "/indexar"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INDEXADO"))
				.andExpect(jsonPath("$.trechos").value(5));
	}

	@Test
	void reindexarComAiServiceForaDoArDevolve503EMantemOStatusFalhou() throws Exception {
		when(aiService.indexar(any(), any(), any()))
				.thenThrow(new ServicoIaException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível conectar ao serviço de IA."));
		int id = criar("a", "texto a");

		mockMvc.perform(post("/api/documentos/" + id + "/indexar"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.title").value("Serviço de IA indisponível"))
				.andExpect(jsonPath("$.detail").value("Não foi possível conectar ao serviço de IA."));
		mockMvc.perform(get("/api/documentos/" + id)).andExpect(jsonPath("$.status").value("FALHOU"));
	}

	@Test
	void reindexarDocumentoInexistenteDevolve404() throws Exception {
		mockMvc.perform(post("/api/documentos/999999/indexar")).andExpect(status().isNotFound());
	}

	@Test
	void removerTambemTiraODocumentoDoIndice() throws Exception {
		int id = criar("temporario", "vai sumir");

		mockMvc.perform(delete("/api/documentos/" + id)).andExpect(status().isNoContent());

		verify(aiService).remover(String.valueOf(id));
	}

	@Test
	void removerFuncionaMesmoComAiServiceForaDoAr() throws Exception {
		doThrow(new ServicoIaException(HttpStatus.SERVICE_UNAVAILABLE, "fora do ar"))
				.when(aiService).remover(any());
		int id = criar("temporario", "vai sumir");

		mockMvc.perform(delete("/api/documentos/" + id)).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/documentos/" + id)).andExpect(status().isNotFound());
	}

	@Test
	void listagemMostraOStatusEOsTrechos() throws Exception {
		criar("a", "texto a");

		mockMvc.perform(get("/api/documentos"))
				.andExpect(jsonPath("$.conteudo[0].status").value("INDEXADO"))
				.andExpect(jsonPath("$.conteudo[0].trechos").value(3));
	}

}
