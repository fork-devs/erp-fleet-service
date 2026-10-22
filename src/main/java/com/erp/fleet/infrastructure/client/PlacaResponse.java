package com.erp.fleet.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Respuesta del hub a la consulta de validacion de una placa.
 *
 * <p>Los campos que no se usan se ignoran: el hub puede anadir los suyos sin
 * que el servicio rompa.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlacaResponse(
        String placa,
        String marca,
        String modelo,
        String vin,
        String numeroMotor,
        String anioFabricacion,
        String color,
        String combustible,
        boolean esValido,
        boolean fromCache
) {
}
