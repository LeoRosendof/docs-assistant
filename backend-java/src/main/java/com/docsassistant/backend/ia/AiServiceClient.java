package com.docsassistant.backend.ia;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.docsassistant.backend.comum.ServicoIaException;

/**
 * Único ponto do backend que conhece o contrato HTTP do ai-service. O resto da
 * aplicação só vê métodos e a ServicoIaException, sem detalhes de rede.
 */
@Component
public class AiServiceClient {

	private final RestClient restClient;

	public AiServiceClient(RestClient aiServiceRestClient) {
		this.restClient = aiServiceRestClient;
	}

	/** Indexa (ou reindexa) o documento e devolve em quantos trechos ele foi dividido. */
	public int indexar(String id, String nome, String texto) {
		IndexacaoIa resposta = executar(() -> restClient.post().uri("/documentos")
				.body(Map.of("id", id, "nome", nome, "texto", texto))
				.retrieve().body(IndexacaoIa.class));
		return resposta.trechos();
	}

	/** Remove o documento do índice. Se ele já não estava lá, não é erro. */
	public void remover(String id) {
		try {
			executar(() -> restClient.delete().uri("/documentos/{id}", id).retrieve().toBodilessEntity());
		}
		catch (ServicoIaException e) {
			if (e.getStatus() != HttpStatus.NOT_FOUND) {
				throw e;
			}
		}
	}

	public RespostaIa perguntar(String pergunta) {
		return executar(() -> restClient.post().uri("/perguntar")
				.body(Map.of("pergunta", pergunta))
				.retrieve().body(RespostaIa.class));
	}

	private <T> T executar(Supplier<T> chamada) {
		try {
			return chamada.get();
		}
		catch (ResourceAccessException e) {
			if (foiTimeout(e)) {
				throw new ServicoIaException(HttpStatus.GATEWAY_TIMEOUT, "O serviço de IA demorou demais para responder.");
			}
			throw new ServicoIaException(HttpStatus.SERVICE_UNAVAILABLE, "Não foi possível conectar ao serviço de IA. Ele está rodando?");
		}
		catch (HttpClientErrorException.NotFound e) {
			throw new ServicoIaException(HttpStatus.NOT_FOUND, "Recurso não encontrado no serviço de IA.");
		}
		catch (RestClientResponseException e) {
			throw new ServicoIaException(HttpStatus.BAD_GATEWAY,
					"O serviço de IA respondeu com erro " + e.getStatusCode().value() + ": " + detalhe(e));
		}
	}

	private static boolean foiTimeout(Throwable erro) {
		for (Throwable t = erro; t != null; t = t.getCause()) {
			if (t instanceof HttpTimeoutException || t instanceof SocketTimeoutException) {
				return true;
			}
		}
		return false;
	}

	// O ai-service devolve {"detail": "..."}; se o corpo vier diferente, usa o texto bruto.
	private static String detalhe(RestClientResponseException e) {
		try {
			Map<?, ?> corpo = e.getResponseBodyAs(Map.class);
			if (corpo != null && corpo.get("detail") instanceof String detail) {
				return detail;
			}
		}
		catch (RuntimeException ignorada) {
			// corpo que não é JSON: cai no texto bruto abaixo
		}
		String bruto = e.getResponseBodyAsString();
		return bruto.length() > 200 ? bruto.substring(0, 200) : bruto;
	}

}
