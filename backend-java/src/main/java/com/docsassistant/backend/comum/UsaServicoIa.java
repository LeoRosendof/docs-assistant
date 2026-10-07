package com.docsassistant.backend.comum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marca as rotas que dependem do ai-service, para o OpenAPI documentar os erros 502, 503 e 504. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface UsaServicoIa {
}
