package com.erp.fleet.application.usecase;

import com.erp.fleet.application.dto.command.UpdateVehicleCommand;
import com.erp.fleet.application.dto.result.VehicleResult;

public interface UpdateVehicleUseCase {
    VehicleResult update(UpdateVehicleCommand command);
}
