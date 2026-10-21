package com.erp.fleet.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Respuesta a una peticion con un principal que no tiene el rol declarado en
 * el endpoint: <strong>403 Forbidden</strong>.
 *
 * <p>La denegacion por rol es una decision de autorizacion y se distingue de
 * un fallo de la operacion: el cliente puede retry con otro rol, pero no con
 * la misma peticion.
 */
@Component
public class ForbiddenAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(ForbiddenAccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws java.io.IOException {
        log.warn("Acceso denegado por rol: {} {}", request.getMethod(), request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://erp-automotive.pe/errors/security");
        body.put("title", "Forbidden");
        body.put("status", HttpServletResponse.SC_FORBIDDEN);
        body.put("detail", "El principal no tiene el rol que exige el endpoint");
        body.put("timestamp", Instant.now().toString());

        response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body));
    }
}
