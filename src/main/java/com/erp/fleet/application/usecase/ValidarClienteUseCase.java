package com.erp.fleet.application.usecase;

import com.erp.fleet.application.dto.command.ValidarClienteCommand;
import com.erp.fleet.application.dto.result.CustomerResult;

/**
 * Valida el documento de un cliente contra el hub.
 *
 * <p>La validacion nunca bloquea el registro: si el hub no esta disponible,
 * el cliente queda pendiente y el intento se reintenta.
 */
public interface ValidarClienteUseCase {

    CustomerResult validar(ValidarClienteCommand command);
}
