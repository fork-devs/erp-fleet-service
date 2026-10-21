package com.erp.fleet.application.usecase;

import java.util.UUID;

/**
 * Reintenta la validacion pendiente de un cliente o de un vehiculo.
 *
 * <p>Un registro ya validado u observado no se vuelve a consultar: no hay
 * nada pendiente de validar.
 */
public interface ReintentarValidacionUseCase {

    /**
     * @return {@code true} si habia una validacion pendiente y se ha
     *         reintentado; {@code false} si el registro ya estaba validado.
     */
    boolean reintentarCliente(UUID customerId, UUID tenantId);

    /**
     * @return {@code true} si habia una validacion pendiente y se ha
     *         reintentado; {@code false} si el registro ya estaba validado.
     */
    boolean reintentarVehiculo(UUID vehicleId, UUID tenantId);
}
