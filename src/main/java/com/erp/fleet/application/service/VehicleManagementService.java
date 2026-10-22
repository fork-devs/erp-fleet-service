package com.erp.fleet.application.service;

import com.erp.fleet.application.dto.command.RegisterVehicleCommand;
import com.erp.fleet.application.dto.command.UpdateVehicleCommand;
import com.erp.fleet.application.dto.result.VehicleResult;
import com.erp.fleet.application.usecase.GetVehicleUseCase;
import com.erp.fleet.application.usecase.RegisterVehicleUseCase;
import com.erp.fleet.application.usecase.UpdateVehicleUseCase;
import com.erp.fleet.domain.exception.CustomerNotFoundException;
import com.erp.fleet.domain.exception.PlateAlreadyExistsException;
import com.erp.fleet.domain.exception.VehicleNotFoundException;
import com.erp.fleet.domain.model.Vehicle;
import com.erp.fleet.domain.port.CustomerRepositoryPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.port.VehicleRepositoryPort;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.Plate;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.domain.valueobject.VehicleId;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class VehicleManagementService implements
        RegisterVehicleUseCase, GetVehicleUseCase, UpdateVehicleUseCase {

    private final VehicleRepositoryPort vehicleRepository;
    private final CustomerRepositoryPort customerRepository;
    private final UnitOfWork unitOfWork;

    public VehicleManagementService(VehicleRepositoryPort vehicleRepository,
                                    CustomerRepositoryPort customerRepository,
                                    UnitOfWork unitOfWork) {
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public VehicleResult register(RegisterVehicleCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            Plate plate = Plate.of(cmd.plate());
            if (vehicleRepository.existsByPlate(plate.getValue(), tenantId)) {
                throw new PlateAlreadyExistsException(plate.getValue());
            }
            CustomerId customerId = new CustomerId(cmd.customerId());
            // El vehiculo pertenece a un cliente DEL TALLER: sin esta
            // comprobacion se podrian colgar vehiculos de un cliente de otro.
            if (customerRepository.findById(customerId, tenantId).isEmpty()) {
                throw new CustomerNotFoundException(cmd.customerId().toString());
            }
            Vehicle vehicle = Vehicle.create(tenantId, customerId,
                    plate, cmd.brand(), cmd.model(), cmd.year(), cmd.vin());
            return toResult(vehicleRepository.save(vehicle));
        });
    }

    @Override
    public VehicleResult getById(UUID vehicleId, UUID tenantId) {
        return unitOfWork.callInTransaction(() ->
                vehicleRepository.findById(new VehicleId(vehicleId), new TenantId(tenantId))
                        .map(this::toResult)
                        .orElseThrow(() -> new VehicleNotFoundException(vehicleId.toString())));
    }

    @Override
    public List<VehicleResult> listAll(UUID tenantId) {
        return unitOfWork.callInTransaction(() ->
                vehicleRepository.findAllByTenantId(new TenantId(tenantId))
                        .stream().map(this::toResult).collect(Collectors.toList()));
    }

    @Override
    public List<VehicleResult> listByCustomer(UUID customerId, UUID tenantId) {
        return unitOfWork.callInTransaction(() ->
                vehicleRepository.findByCustomerId(new CustomerId(customerId), new TenantId(tenantId))
                        .stream().map(this::toResult).collect(Collectors.toList()));
    }

    @Override
    public VehicleResult update(UpdateVehicleCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(cmd.tenantId());
            Vehicle vehicle = vehicleRepository.findById(new VehicleId(cmd.vehicleId()), tenantId)
                    .orElseThrow(() -> new VehicleNotFoundException(cmd.vehicleId().toString()));
            vehicle.update(cmd.brand(), cmd.model(), cmd.year(), cmd.vin());
            return toResult(vehicleRepository.save(vehicle));
        });
    }

    private VehicleResult toResult(Vehicle v) {
        return new VehicleResult(
                v.getId().getValue(), v.getTenantId().getValue(), v.getCustomerId().getValue(),
                v.getPlate().getValue(), v.getBrand(), v.getModel(), v.getYear(), v.getVin(),
                v.getValidationStatus().name(), v.getStatus().name(), v.getCreatedAt()
        );
    }
}
