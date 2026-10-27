package com.erp.fleet.application.usecase;

import com.erp.fleet.application.dto.command.ValidateVehicleCommand;
import com.erp.fleet.application.dto.result.VehicleResult;

/**
 * Use case to validate vehicle plate against the integration hub.
 */
public interface ValidateVehicleUseCase {

    VehicleResult validate(ValidateVehicleCommand command);

    // Compatibility alias
    default VehicleResult validar(ValidateVehicleCommand command) {
        return validate(command);
    }
}
