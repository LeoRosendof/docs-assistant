package com.docsassistant.backend.ia;

/** Resposta do ai-service ao indexar: quantos trechos o documento virou. */
record IndexacaoIa(String id, String nome, int trechos) {
}
