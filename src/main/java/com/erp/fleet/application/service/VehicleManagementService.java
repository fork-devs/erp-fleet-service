package com.erp.fleet.application.service;

import com.erp.fleet.application.dto.command.RegisterVehicleCommand;
import com.erp.fleet.application.dto.command.UpdateVehicleCommand;
import com.erp.fleet.application.dto.result.VehicleResult;
import com.erp.fleet.application.usecase.GetVehicleUseCase;
import com.erp.fleet.application.usecase.RegisterVehicleUseCase;
import com.erp.fleet.application.usecase.UpdateVehicleUseCase;
import com.erp.fleet.domain.exception.PlateAlreadyExistsException;
import com.erp.fleet.domain.exception.VehicleNotFoundException;
import com.erp.fleet.domain.model.Vehicle;
import com.erp.fleet.domain.port.VehicleRepositoryPort;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.Plate;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.domain.valueobject.VehicleId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VehicleManagementService implements
        RegisterVehicleUseCase, GetVehicleUseCase, UpdateVehicleUseCase {

    private final VehicleRepositoryPort vehicleRepository;

    public VehicleManagementService(VehicleRepositoryPort vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    @Transactional
    public VehicleResult register(RegisterVehicleCommand cmd) {
        TenantId tenantId = new TenantId(cmd.tenantId());
        Plate plate = Plate.of(cmd.plate());
        if (vehicleRepository.existsByPlate(plate.getValue(), tenantId))
            throw new PlateAlreadyExistsException(plate.getValue());
        Vehicle vehicle = Vehicle.create(tenantId, new CustomerId(cmd.customerId()),
                plate, cmd.brand(), cmd.model(), cmd.year(), cmd.vin());
        return toResult(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResult getById(UUID vehicleId, UUID tenantId) {
        return vehicleRepository.findById(new VehicleId(vehicleId), new TenantId(tenantId))
                .map(this::toResult)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResult> listAll(UUID tenantId) {
        return vehicleRepository.findAllByTenantId(new TenantId(tenantId))
                .stream().map(this::toResult).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResult> listByCustomer(UUID customerId, UUID tenantId) {
        return vehicleRepository.findByCustomerId(new CustomerId(customerId), new TenantId(tenantId))
                .stream().map(this::toResult).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VehicleResult update(UpdateVehicleCommand cmd) {
        TenantId tenantId = new TenantId(cmd.tenantId());
        Vehicle vehicle = vehicleRepository.findById(new VehicleId(cmd.vehicleId()), tenantId)
                .orElseThrow(() -> new VehicleNotFoundException(cmd.vehicleId().toString()));
        vehicle.update(cmd.brand(), cmd.model(), cmd.year(), cmd.vin());
        return toResult(vehicleRepository.save(vehicle));
    }

    private VehicleResult toResult(Vehicle v) {
        return new VehicleResult(
                v.getId().getValue(), v.getTenantId().getValue(), v.getCustomerId().getValue(),
                v.getPlate().getValue(), v.getBrand(), v.getModel(), v.getYear(), v.getVin(),
                v.getValidationStatus().name(), v.getStatus().name(), v.getCreatedAt()
        );
    }
}
