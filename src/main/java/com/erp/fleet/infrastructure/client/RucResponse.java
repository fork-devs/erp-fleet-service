package com.erp.fleet.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Respuesta del hub a la consulta de validacion de un RUC.
 *
 * <p>Los campos que no se usan se ignoran: el hub puede anadir los suyos sin
 * que el servicio rompa.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RucResponse(
        String ruc,
        String razonSocial,
        String estado,
        String condicion,
        String direccionFiscal,
        boolean esValido,
        boolean fromCache
) {
}
