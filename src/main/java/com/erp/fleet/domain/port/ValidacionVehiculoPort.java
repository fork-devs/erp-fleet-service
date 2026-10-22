package com.erp.fleet.domain.port;

import com.erp.fleet.domain.model.ResultadoValidacionVehiculo;

/**
 * Consulta al hub la validacion de una placa.
 *
 * <p>Igual que en la identidad, el fallo del hub no es un resultado: es un
 * intento que se reintenta, y lo expresa una excepcion.
 */
public interface ValidacionVehiculoPort {

    ResultadoValidacionVehiculo validarPlaca(String placa);
}
