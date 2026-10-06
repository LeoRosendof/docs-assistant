package com.docsassistant.backend.ia;

import java.util.List;

public record RespostaIa(String resposta, boolean truncada, List<FonteIa> fontes) {
}
