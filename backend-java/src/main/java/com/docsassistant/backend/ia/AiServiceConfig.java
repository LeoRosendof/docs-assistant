package com.docsassistant.backend.ia;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class AiServiceConfig {

	// O timeout de leitura é folgado de propósito: a primeira pergunta pode levar ~35 s
	// só para o modelo carregar na GPU.
	@Bean
	RestClient aiServiceRestClient(
			@Value("${ai-service.url}") String url,
			@Value("${ai-service.connect-timeout}") Duration connectTimeout,
			@Value("${ai-service.read-timeout}") Duration readTimeout) {
		// HTTP/1.1 explícito: por padrão o cliente do Java tenta subir para HTTP/2 (Upgrade: h2c)
		// e, contra o uvicorn, o corpo do POST chegava vazio (422 "Field required").
		HttpClient httpClient = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1)
				.connectTimeout(connectTimeout)
				.build();
		JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
		fabrica.setReadTimeout(readTimeout);
		return RestClient.builder().baseUrl(url).requestFactory(fabrica).build();
	}

}
