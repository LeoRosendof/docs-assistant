package com.docsassistant.backend.ia;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;

/**
 * Regressão de um bug que os testes com servidor simulado não podiam pegar: o cliente HTTP do
 * Java tenta por padrão subir para HTTP/2 (cabeçalho "Upgrade: h2c") e, contra o uvicorn do
 * ai-service, o corpo do POST chegava vazio (422 "Field required"). Aqui um servidor HTTP real
 * confere o que de fato vai pela rede.
 */
class AiServiceConfigTest {

	private HttpServer servidor;
	private final AtomicReference<Headers> cabecalhos = new AtomicReference<>();
	private final AtomicReference<String> corpo = new AtomicReference<>();

	@BeforeEach
	void subirServidor() throws Exception {
		servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		servidor.createContext("/documentos", troca -> {
			cabecalhos.set(troca.getRequestHeaders());
			corpo.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] resposta = "{\"id\":\"7\",\"nome\":\"a\",\"trechos\":2}".getBytes(StandardCharsets.UTF_8);
			troca.getResponseHeaders().add("Content-Type", "application/json");
			troca.sendResponseHeaders(201, resposta.length);
			troca.getResponseBody().write(resposta);
			troca.close();
		});
		servidor.start();
	}

	@AfterEach
	void pararServidor() {
		servidor.stop(0);
	}

	@Test
	void clienteFalaHttp11SemPedirUpgradeParaHttp2() {
		RestClient restClient = new AiServiceConfig().aiServiceRestClient(
				"http://localhost:" + servidor.getAddress().getPort(), Duration.ofSeconds(2), Duration.ofSeconds(5));

		int trechos = new AiServiceClient(restClient).indexar("7", "a", "texto");

		assertThat(trechos).isEqualTo(2);
		assertThat(corpo.get()).contains("\"texto\":\"texto\"");
		assertThat(cabecalhos.get().containsKey("Upgrade")).as("não deve pedir upgrade para h2c").isFalse();
		assertThat(cabecalhos.get().containsKey("Http2-settings")).isFalse();
	}

}
