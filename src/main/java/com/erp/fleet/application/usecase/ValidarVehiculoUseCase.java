package com.erp.fleet.application.usecase;

import com.erp.fleet.application.dto.command.ValidarVehiculoCommand;
import com.erp.fleet.application.dto.result.VehicleResult;

/**
 * Valida la placa de un vehiculo contra el hub.
 *
 * <p>La validacion nunca bloquea el registro: si el hub no esta disponible,
 * el vehiculo queda pendiente y el intento se reintenta.
 */
public interface ValidarVehiculoUseCase {

    VehicleResult validar(ValidarVehiculoCommand command);
}
