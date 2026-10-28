package com.erp.fleet.domain.port;

import com.erp.fleet.domain.model.VehicleValidationResult;

/**
 * Port to validate vehicle license plates against the integration hub.
 */
public interface VehicleValidationPort {

    VehicleValidationResult validatePlate(String plate);

    // Compatibility alias
    default VehicleValidationResult validarPlaca(String plate) {
        return validatePlate(plate);
    }
}
