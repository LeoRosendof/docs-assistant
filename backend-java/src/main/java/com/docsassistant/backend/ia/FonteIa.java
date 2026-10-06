package com.docsassistant.backend.ia;

/** Trecho de documento que o ai-service usou para montar uma resposta. */
public record FonteIa(String documento, int posicao, String texto, double score) {
}
