package com.erp.fleet.infrastructure.persistence.mapper;

import com.erp.fleet.domain.model.ValidationStatus;
import com.erp.fleet.domain.model.Vehicle;
import com.erp.fleet.domain.model.VehicleStatus;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.Plate;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.domain.valueobject.VehicleId;
import com.erp.fleet.infrastructure.persistence.entity.VehicleJpaEntity;

public class VehiclePersistenceMapper {
    private VehiclePersistenceMapper() {}

    public static VehicleJpaEntity toEntity(Vehicle v) {
        return new VehicleJpaEntity(
                v.getId().getValue(), v.getTenantId().getValue(), v.getCustomerId().getValue(),
                v.getPlate().getValue(), v.getBrand(), v.getModel(), v.getYear(), v.getVin(),
                v.getValidationStatus().name(), v.getStatus().name(), v.getCreatedAt()
        );
    }

    public static Vehicle toDomain(VehicleJpaEntity e) {
        return new Vehicle(
                new VehicleId(e.getId()), new TenantId(e.getTenantId()), new CustomerId(e.getCustomerId()),
                Plate.of(e.getPlate()), e.getBrand(), e.getModel(), e.getYear(), e.getVin(),
                ValidationStatus.valueOf(e.getValidationStatus()),
                VehicleStatus.valueOf(e.getStatus()), e.getCreatedAt()
        );
    }
}
