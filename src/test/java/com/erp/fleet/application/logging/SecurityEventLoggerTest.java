package com.erp.fleet.application.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El registro de eventos de seguridad tiene un esquema uniforme y no admite
 * credenciales.
 *
 * <p>Un registro de auditoria con credenciales es un almacen de credenciales:
 * por eso el esquema no tiene ningun parametro para ellas, y no pueden
 * colarse por descuido.
 */
class SecurityEventLoggerTest {

    @Test
    @DisplayName("El esquema lleva evento, taller, usuario, traza, resultado y marca de tiempo")
    void elEsquemaLlevaLosCamposDelDiseno() {
        UUID taller = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();

        String linea = SecurityEventLogger.formatear("CLIENTE_ALTA", taller, usuario,
                "traza-123", "OK", Map.of("cliente_id", "c-1"));

        assertTrue(linea.startsWith("evento=CLIENTE_ALTA "), linea);
        assertTrue(linea.contains("tenant_id=" + taller), linea);
        assertTrue(linea.contains("user_id=" + usuario), linea);
        assertTrue(linea.contains("trace_id=traza-123"), linea);
        assertTrue(linea.contains("resultado=OK"), linea);
        assertTrue(linea.contains("timestamp="), linea);
        assertTrue(linea.contains("cliente_id=c-1"), linea);
    }

    @Test
    @DisplayName("Los campos ausentes se rellenan con un guion, no con un null")
    void losCamposAusentesSeRellenanConUnGuas() {
        String linea = SecurityEventLogger.formatear("ACCESO_DENEGADO", null, null, null, "DENEGADO", null);

        assertTrue(linea.contains("tenant_id=-"), linea);
        assertTrue(linea.contains("user_id=-"), linea);
        assertTrue(linea.contains("trace_id=-"), linea);
        assertFalse(linea.contains("null"), linea);
    }

    @Test
    @DisplayName("El esquema no tiene ningun parametro para credenciales")
    void elEsquemaNoTieneNingunParametroParaCredenciales() {
        java.lang.reflect.Method[] metodos = SecurityEventLogger.class.getDeclaredMethods();
        String firma = java.util.Arrays.stream(metodos)
                .map(java.lang.reflect.Method::getParameterTypes)
                .flatMap(java.util.Arrays::stream)
                .map(Class::getSimpleName)
                .toList()
                .toString();

        String enMinusculas = firma.toLowerCase();
        assertFalse(enMinusculas.contains("password"), firma);
        assertFalse(enMinusculas.contains("token"), firma);
        assertFalse(enMinusculas.contains("secret"), firma);
        assertFalse(enMinusculas.contains("credential"), firma);
    }
}
