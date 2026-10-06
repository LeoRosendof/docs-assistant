package com.docsassistant.backend.pergunta;

/** Trecho de documento em que a resposta se baseou, para o usuário poder conferir. */
public record FonteResponse(String documento, int posicao, String texto, double score) {
}
