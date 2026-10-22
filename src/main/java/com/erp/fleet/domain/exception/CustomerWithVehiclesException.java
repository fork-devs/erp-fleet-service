package com.erp.fleet.domain.exception;

/**
 * Se lanza cuando se intenta dar de baja un cliente que tiene vehiculos
 * asociados.
 *
 * <p>La baja es logica: el registro se conserva. Si el cliente desaparece, sus
 * vehiculos quedan apuntando a un cliente dado de baja y la orden de trabajo
 * de un taller se queda sin propietario.
 */
public class CustomerWithVehiclesException extends DomainException {

    public CustomerWithVehiclesException(String customerId) {
        super("FLEET_CUSTOMER_HAS_VEHICLES",
                "El cliente " + customerId + " tiene vehiculos asociados y no se puede dar de baja");
    }
}
