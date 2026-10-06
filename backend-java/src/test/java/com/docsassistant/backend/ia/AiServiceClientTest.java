package com.docsassistant.backend.ia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.docsassistant.backend.comum.ServicoIaException;

// Testa o contrato HTTP com o ai-service sem subir o ai-service: o que o backend envia
// e como ele traduz cada tipo de falha.
class AiServiceClientTest {

	private MockRestServiceServer servidor;
	private AiServiceClient cliente;

	@BeforeEach
	void montar() {
		RestClient.Builder builder = RestClient.builder().baseUrl("http://ia");
		servidor = MockRestServiceServer.bindTo(builder).build();
		cliente = new AiServiceClient(builder.build());
	}

	@Test
	void indexarEnviaIdNomeETextoEDevolveOsTrechos() {
		servidor.expect(requestTo("http://ia/documentos"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.id").value("7"))
				.andExpect(jsonPath("$.nome").value("guia.txt"))
				.andExpect(jsonPath("$.texto").value("conteúdo"))
				.andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
						.body("{\"id\":\"7\",\"nome\":\"guia.txt\",\"trechos\":4}"));

		assertThat(cliente.indexar("7", "guia.txt", "conteúdo")).isEqualTo(4);
		servidor.verify();
	}

	@Test
	void perguntarDevolveARespostaComAsFontes() {
		servidor.expect(requestTo("http://ia/perguntar"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(jsonPath("$.pergunta").value("Quantos pontos?"))
				.andRespond(withSuccess(
						"{\"resposta\":\"0,5 ponto [1]\",\"truncada\":false,"
								+ "\"fontes\":[{\"documento\":\"rsl\",\"posicao\":2,"
								+ "\"texto\":\"Parcialmente vale 0,5\",\"score\":0.65}]}",
						MediaType.APPLICATION_JSON));

		RespostaIa resposta = cliente.perguntar("Quantos pontos?");

		assertThat(resposta.resposta()).isEqualTo("0,5 ponto [1]");
		assertThat(resposta.truncada()).isFalse();
		assertThat(resposta.fontes()).containsExactly(new FonteIa("rsl", 2, "Parcialmente vale 0,5", 0.65));
	}

	@Test
	void removerUsaDeleteComOIdNaUrl() {
		servidor.expect(requestTo("http://ia/documentos/7")).andExpect(method(HttpMethod.DELETE))
				.andRespond(withStatus(HttpStatus.NO_CONTENT));

		cliente.remover("7");

		servidor.verify();
	}

	@Test
	void removerDocumentoQueJaNaoEstavaNoIndiceNaoEErro() {
		servidor.expect(requestTo("http://ia/documentos/7")).andRespond(withResourceNotFound());

		assertThatCode(() -> cliente.remover("7")).doesNotThrowAnyException();
	}

	@Test
	void respostaDeErroDoAiServiceVira502ComODetalheDele() {
		servidor.expect(requestTo("http://ia/perguntar"))
				.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE).contentType(MediaType.APPLICATION_JSON)
						.body("{\"detail\":\"Não foi possível conectar ao Ollama. Ele está rodando?\"}"));

		assertThatThrownBy(() -> cliente.perguntar("oi"))
				.isInstanceOfSatisfying(ServicoIaException.class, e -> {
					assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
					assertThat(e.getMessage()).contains("503").contains("Ollama");
				});
	}

	@Test
	void corpoDeErroQueNaoEJsonNaoQuebraOTratamento() {
		servidor.expect(requestTo("http://ia/perguntar"))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.TEXT_PLAIN)
						.body("Internal Server Error"));

		assertThatThrownBy(() -> cliente.perguntar("oi"))
				.isInstanceOfSatisfying(ServicoIaException.class, e -> {
					assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
					assertThat(e.getMessage()).contains("500").contains("Internal Server Error");
				});
	}

	@Test
	void conexaoRecusadaVira503() {
		servidor.expect(requestTo("http://ia/perguntar"))
				.andRespond(request -> {
					throw new ConnectException("Connection refused");
				});

		assertThatThrownBy(() -> cliente.perguntar("oi"))
				.isInstanceOfSatisfying(ServicoIaException.class,
						e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
	}

	@Test
	void timeoutVira504() {
		servidor.expect(requestTo("http://ia/perguntar"))
				.andRespond(request -> {
					throw new HttpTimeoutException("request timed out");
				});

		assertThatThrownBy(() -> cliente.perguntar("oi"))
				.isInstanceOfSatisfying(ServicoIaException.class,
						e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT));
	}

}
