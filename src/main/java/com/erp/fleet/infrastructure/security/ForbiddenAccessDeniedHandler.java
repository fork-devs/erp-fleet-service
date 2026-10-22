package com.erp.fleet.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws java.io.IOException {
        // El acceso denegado es un EVENTO de seguridad: se registra con el
        // mismo esquema que el resto, para que se pueda consultar igual que
        // los demas.
        com.erp.fleet.application.logging.SecurityEventLogger.registrar(
                "ACCESO_DENEGADO",
                com.erp.fleet.infrastructure.persistence.rls.TenantContextHolder.getTenantId(),
                null,
                org.slf4j.MDC.get("trace_id"),
                "DENEGADO",
                java.util.Map.of("ruta", request.getRequestURI(), "metodo", request.getMethod()));

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
