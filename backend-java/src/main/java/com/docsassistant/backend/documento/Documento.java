package com.docsassistant.backend.documento;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;

@Entity
public class Documento {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 200)
	private String nome;

	@Lob
	@Column(nullable = false)
	private String texto;

	// Guardado à parte para a listagem não precisar carregar o texto inteiro.
	@Column(nullable = false)
	private int caracteres;

	@Column(nullable = false, updatable = false)
	private Instant criadoEm;

	protected Documento() {
	}

	public Documento(String nome, String texto) {
		this.nome = nome;
		this.texto = texto;
		this.caracteres = texto.length();
	}

	@PrePersist
	void aoCriar() {
		this.criadoEm = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getTexto() {
		return texto;
	}

	public int getCaracteres() {
		return caracteres;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

}
