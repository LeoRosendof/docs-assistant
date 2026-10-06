package com.docsassistant.backend.comum;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Todos os erros da API saem no mesmo formato (RFC 7807, application/problem+json):
 * status, title, detail e instance, mais a lista "erros" quando há campos inválidos.
 * Estender ResponseEntityExceptionHandler cobre os erros do próprio Spring MVC (rota
 * inexistente, método não permitido, id não numérico...) sem tratar um por um.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	ProblemDetail recursoNaoEncontrado(RecursoNaoEncontradoException ex) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problema.setTitle(titulo(HttpStatus.NOT_FOUND.value()));
		return problema;
	}

	// Rede de segurança: o cliente nunca vê stack trace nem mensagem interna.
	@ExceptionHandler(Exception.class)
	ProblemDetail erroInesperado(Exception ex) {
		log.error("Erro inesperado", ex);
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(
				HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro inesperado. Tente novamente mais tarde.");
		problema.setTitle(titulo(HttpStatus.INTERNAL_SERVER_ERROR.value()));
		return problema;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<Map<String, String>> erros = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> Map.of("campo", e.getField(), "mensagem", Objects.toString(e.getDefaultMessage(), "inválido")))
				.toList();
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, "Há campos inválidos na requisição.");
		problema.setProperty("erros", erros);
		return handleExceptionInternal(ex, problema, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, "O corpo da requisição está ausente ou não é um JSON válido.");
		return handleExceptionInternal(ex, problema, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleTypeMismatch(
			TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		String parametro = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
		ProblemDetail problema = ProblemDetail.forStatusAndDetail(
				status, "O valor '" + ex.getValue() + "' não é válido para '" + parametro + "'.");
		return handleExceptionInternal(ex, problema, headers, status, request);
	}

	// Padroniza o título em português, inclusive para os erros que o Spring monta sozinho.
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
		ResponseEntity<Object> resposta = super.handleExceptionInternal(ex, body, headers, statusCode, request);
		if (resposta != null && resposta.getBody() instanceof ProblemDetail problema) {
			problema.setTitle(titulo(statusCode.value()));
		}
		return resposta;
	}

	private static String titulo(int status) {
		return switch (status) {
			case 400 -> "Requisição inválida";
			case 404 -> "Recurso não encontrado";
			case 405 -> "Método não permitido";
			case 415 -> "Tipo de conteúdo não suportado";
			case 500 -> "Erro interno";
			default -> "Erro";
		};
	}

}
