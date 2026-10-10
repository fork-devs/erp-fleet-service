package com.erp.fleet.application.usecase;

import com.erp.fleet.application.dto.command.RegisterVehicleCommand;
import com.erp.fleet.application.dto.result.VehicleResult;

public interface RegisterVehicleUseCase {
    VehicleResult register(RegisterVehicleCommand command);
}
