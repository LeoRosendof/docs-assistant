package com.docsassistant.backend.pergunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.hamcrest.Matchers;
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
import com.docsassistant.backend.ia.FonteIa;
import com.docsassistant.backend.ia.RespostaIa;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PerguntaControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PerguntaRepository repository;

	@MockitoBean
	private AiServiceClient aiService;

	@BeforeEach
	void iaResponde() {
		when(aiService.perguntar(any())).thenReturn(new RespostaIa(
				"Parcialmente vale 0,5 ponto [1].", false,
				List.of(new FonteIa("rsl.pdf", 2, "Parcialmente, 0,5 pontos", 0.65))));
	}

	private void perguntar(String pergunta) throws Exception {
		mockMvc.perform(post("/api/perguntas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"pergunta\":\"" + pergunta + "\"}")).andExpect(status().isCreated());
	}

	@Test
	void perguntarDevolve201ComRespostaFontesELocation() throws Exception {
		mockMvc.perform(post("/api/perguntas").contentType(MediaType.APPLICATION_JSON)
				.content("{\"pergunta\":\"Quantos pontos vale Parcialmente?\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", Matchers.startsWith("/api/perguntas/")))
				.andExpect(jsonPath("$.pergunta").value("Quantos pontos vale Parcialmente?"))
				.andExpect(jsonPath("$.resposta").value("Parcialmente vale 0,5 ponto [1]."))
				.andExpect(jsonPath("$.truncada").value(false))
				.andExpect(jsonPath("$.fontes[0].documento").value("rsl.pdf"))
				.andExpect(jsonPath("$.fontes[0].posicao").value(2))
				.andExpect(jsonPath("$.fontes[0].score").value(0.65));
	}

	@Test
	void perguntaFicaNoHistoricoSemGuardarAsFontes() throws Exception {
		perguntar("primeira");
		perguntar("segunda");

		mockMvc.perform(get("/api/perguntas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElementos").value(2))
				.andExpect(jsonPath("$.conteudo[0].resposta").value("Parcialmente vale 0,5 ponto [1]."))
				.andExpect(jsonPath("$.conteudo[0].fontes").doesNotExist());
	}

	@Test
	void buscarPerguntaInexistenteDevolve404() throws Exception {
		mockMvc.perform(get("/api/perguntas/999999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Pergunta 999999 não encontrada."));
	}

	@Test
	void perguntaEmBrancoDevolve400SemChamarAIa() throws Exception {
		mockMvc.perform(post("/api/perguntas").contentType(MediaType.APPLICATION_JSON).content("{\"pergunta\":\"  \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erros[0].campo").value("pergunta"))
				.andExpect(jsonPath("$.erros[0].mensagem").value("é obrigatória"));

		verify(aiService, never()).perguntar(any());
	}

	@Test
	void seAIaFalharNadaEGravadoNoHistorico() throws Exception {
		when(aiService.perguntar(any())).thenThrow(
				new ServicoIaException(HttpStatus.GATEWAY_TIMEOUT, "O serviço de IA demorou demais para responder."));

		mockMvc.perform(post("/api/perguntas").contentType(MediaType.APPLICATION_JSON).content("{\"pergunta\":\"oi\"}"))
				.andExpect(status().isGatewayTimeout())
				.andExpect(jsonPath("$.title").value("Serviço de IA demorou demais"));

		assertThat(repository.count()).isZero();
	}

}
