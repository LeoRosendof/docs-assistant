package com.docsassistant.backend.pergunta;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;

/** Uma pergunta feita ao assistente e a resposta que ele deu (o histórico). */
@Entity
public class Pergunta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 2000)
	private String pergunta;

	@Lob
	@Column(nullable = false)
	private String resposta;

	@Column(nullable = false)
	private boolean truncada;

	@Column(nullable = false, updatable = false)
	private Instant criadoEm;

	protected Pergunta() {
	}

	public Pergunta(String pergunta, String resposta, boolean truncada) {
		this.pergunta = pergunta;
		this.resposta = resposta;
		this.truncada = truncada;
	}

	@PrePersist
	void aoCriar() {
		this.criadoEm = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getPergunta() {
		return pergunta;
	}

	public String getResposta() {
		return resposta;
	}

	public boolean isTruncada() {
		return truncada;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

}
