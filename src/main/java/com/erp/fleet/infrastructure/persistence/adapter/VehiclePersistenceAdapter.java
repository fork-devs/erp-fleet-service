package com.erp.fleet.infrastructure.persistence.adapter;

import com.erp.fleet.domain.model.Vehicle;
import com.erp.fleet.domain.port.VehicleRepositoryPort;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.domain.valueobject.VehicleId;
import com.erp.fleet.infrastructure.persistence.mapper.VehiclePersistenceMapper;
import com.erp.fleet.infrastructure.persistence.repository.SpringDataVehicleRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class VehiclePersistenceAdapter implements VehicleRepositoryPort {
    private final SpringDataVehicleRepository repo;

    public VehiclePersistenceAdapter(SpringDataVehicleRepository repo) { this.repo = repo; }

    @Override
    public Vehicle save(Vehicle vehicle) {
        return VehiclePersistenceMapper.toDomain(repo.save(VehiclePersistenceMapper.toEntity(vehicle)));
    }

    @Override
    public Optional<Vehicle> findById(VehicleId id, TenantId tenantId) {
        return repo.findByIdAndTenantId(id.getValue(), tenantId.getValue())
                .map(VehiclePersistenceMapper::toDomain);
    }

    @Override
    public List<Vehicle> findAllByTenantId(TenantId tenantId) {
        return repo.findAllByTenantId(tenantId.getValue()).stream()
                .map(VehiclePersistenceMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<Vehicle> findByCustomerId(CustomerId customerId, TenantId tenantId) {
        return repo.findByCustomerIdAndTenantId(customerId.getValue(), tenantId.getValue()).stream()
                .map(VehiclePersistenceMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByPlate(String plate, TenantId tenantId) {
        return repo.existsByPlateAndTenantId(plate, tenantId.getValue());
    }
}
