package com.erp.fleet.application.service;

import com.erp.fleet.application.dto.command.ValidateCustomerCommand;
import com.erp.fleet.application.dto.command.ValidateVehicleCommand;
import com.erp.fleet.application.dto.result.CustomerResult;
import com.erp.fleet.application.dto.result.VehicleResult;
import com.erp.fleet.application.usecase.RetryValidationUseCase;
import com.erp.fleet.application.usecase.ValidateCustomerUseCase;
import com.erp.fleet.application.usecase.ValidateVehicleUseCase;
import com.erp.fleet.domain.exception.CustomerNotFoundException;
import com.erp.fleet.domain.exception.VehicleNotFoundException;
import com.erp.fleet.domain.model.Customer;
import com.erp.fleet.domain.model.IdentityValidationResult;
import com.erp.fleet.domain.model.ValidationStatus;
import com.erp.fleet.domain.model.Vehicle;
import com.erp.fleet.domain.model.VehicleValidationResult;
import com.erp.fleet.domain.port.CustomerRepositoryPort;
import com.erp.fleet.domain.port.IdentityValidationPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.port.ValidationCachePort;
import com.erp.fleet.domain.port.VehicleRepositoryPort;
import com.erp.fleet.domain.port.VehicleValidationPort;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.domain.valueobject.VehicleId;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service orchestrating customer identity and vehicle plate validation against the integration hub.
 */
public class ValidationService implements
        ValidateCustomerUseCase, ValidateVehicleUseCase, RetryValidationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ValidationService.class);

    private static final Duration INITIAL_BACKOFF = Duration.ofMinutes(1);
    private static final Duration MAX_BACKOFF = Duration.ofHours(24);

    private final CustomerRepositoryPort customerRepository;
    private final VehicleRepositoryPort vehicleRepository;
    private final IdentityValidationPort identityValidation;
    private final VehicleValidationPort vehicleValidation;
    private final ValidationCachePort cache;
    private final UnitOfWork unitOfWork;

    public ValidationService(CustomerRepositoryPort customerRepository,
                             VehicleRepositoryPort vehicleRepository,
                             IdentityValidationPort identityValidation,
                             VehicleValidationPort vehicleValidation,
                             ValidationCachePort cache,
                             UnitOfWork unitOfWork) {
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.identityValidation = identityValidation;
        this.vehicleValidation = vehicleValidation;
        this.cache = cache;
        this.unitOfWork = unitOfWork;
    }

    @Override
    @CircuitBreaker(name = "hubValidation", fallbackMethod = "validateCustomerFallback")
    public CustomerResult validate(ValidateCustomerCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            Customer customer = customerRepository
                    .findById(new CustomerId(cmd.customerId()), new TenantId(cmd.tenantId()))
                    .orElseThrow(() -> new CustomerNotFoundException(cmd.customerId().toString()));

            String documentNumber = customer.getDocNumber().getValue();
            Optional<IdentityValidationResult> cached = cache.getRuc(documentNumber);

            IdentityValidationResult result = cached.orElseGet(() -> {
                IdentityValidationResult queried = identityValidation.validateDocument(documentNumber);
                cache.putRuc(documentNumber, queried);
                return queried;
            });

            if (result.isValid()) {
                customer.markValidated(result.getLegalName(), result.getTaxStatus(),
                        result.getTaxCondition(), result.getFiscalAddress(), Instant.now());
            } else {
                customer.markObserved();
            }

            return toCustomerResult(customerRepository.save(customer));
        });
    }

    @SuppressWarnings("unused")
    private CustomerResult validateCustomerFallback(ValidateCustomerCommand cmd, Throwable cause) {
        log.warn("Customer validation for {} degraded to pending: {}",
                cmd.customerId(), cause.getMessage());
        return unitOfWork.callInTransaction(() -> {
            Customer customer = customerRepository
                    .findById(new CustomerId(cmd.customerId()), new TenantId(cmd.tenantId()))
                    .orElseThrow(() -> new CustomerNotFoundException(cmd.customerId().toString()));
            customer.registerValidationAttempt(nextAttempt(customer.getValidationAttempts()));
            return toCustomerResult(customerRepository.save(customer));
        });
    }

    // Compatibility fallback alias
    @SuppressWarnings("unused")
    private CustomerResult validarClienteDegradado(ValidateCustomerCommand cmd, Throwable cause) {
        return validateCustomerFallback(cmd, cause);
    }

    @Override
    @CircuitBreaker(name = "hubValidation", fallbackMethod = "validateVehicleFallback")
    public VehicleResult validate(ValidateVehicleCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            Vehicle vehicle = vehicleRepository
                    .findById(new VehicleId(cmd.vehicleId()), new TenantId(cmd.tenantId()))
                    .orElseThrow(() -> new VehicleNotFoundException(cmd.vehicleId().toString()));

            String plate = vehicle.getPlate().getValue();
            Optional<VehicleValidationResult> cached = cache.getPlate(plate);

            VehicleValidationResult result = cached.orElseGet(() -> {
                VehicleValidationResult queried = vehicleValidation.validatePlate(plate);
                cache.putPlate(plate, queried);
                return queried;
            });

            if (result.isValid()) {
                vehicle.markValidated(result.getVin(), result.getEngineNumber(),
                        result.getBrand(), result.getModel(), result.getColor(),
                        result.getFuelType(), Instant.now());
            } else {
                vehicle.markObserved();
            }

            return toVehicleResult(vehicleRepository.save(vehicle));
        });
    }

    @SuppressWarnings("unused")
    private VehicleResult validateVehicleFallback(ValidateVehicleCommand cmd, Throwable cause) {
        log.warn("Vehicle validation for {} degraded to pending: {}",
                cmd.vehicleId(), cause.getMessage());
        return unitOfWork.callInTransaction(() -> {
            Vehicle vehicle = vehicleRepository
                    .findById(new VehicleId(cmd.vehicleId()), new TenantId(cmd.tenantId()))
                    .orElseThrow(() -> new VehicleNotFoundException(cmd.vehicleId().toString()));
            vehicle.registerValidationAttempt(nextAttempt(vehicle.getValidationAttempts()));
            return toVehicleResult(vehicleRepository.save(vehicle));
        });
    }

    // Compatibility fallback alias
    @SuppressWarnings("unused")
    private VehicleResult validarVehiculoDegradado(ValidateVehicleCommand cmd, Throwable cause) {
        return validateVehicleFallback(cmd, cause);
    }

    @Override
    public boolean retryCustomer(UUID customerId, UUID tenantId) {
        return Boolean.TRUE.equals(unitOfWork.callInTransaction(() -> {
            Customer customer = customerRepository
                    .findById(new CustomerId(customerId), new TenantId(tenantId))
                    .orElseThrow(() -> new CustomerNotFoundException(customerId.toString()));

            if (customer.getValidationStatus() != ValidationStatus.PENDING) {
                return false;
            }

            cache.evictRuc(customer.getDocNumber().getValue());
            validate(new ValidateCustomerCommand(customerId, tenantId));
            return true;
        }));
    }

    @Override
    public boolean retryVehicle(UUID vehicleId, UUID tenantId) {
        return Boolean.TRUE.equals(unitOfWork.callInTransaction(() -> {
            Vehicle vehicle = vehicleRepository
                    .findById(new VehicleId(vehicleId), new TenantId(tenantId))
                    .orElseThrow(() -> new VehicleNotFoundException(vehicleId.toString()));

            if (vehicle.getValidationStatus() != ValidationStatus.PENDING) {
                return false;
            }

            cache.evictPlate(vehicle.getPlate().getValue());
            validate(new ValidateVehicleCommand(vehicleId, tenantId));
            return true;
        }));
    }

    private Instant nextAttempt(int previousAttempts) {
        long multiplier = 1L << Math.min(previousAttempts, 10);
        long seconds = Math.min(INITIAL_BACKOFF.getSeconds() * multiplier, MAX_BACKOFF.getSeconds());
        return Instant.now().plus(Duration.ofSeconds(seconds));
    }

    private CustomerResult toCustomerResult(Customer c) {
        return new CustomerResult(
                c.getId().getValue(),
                c.getTenantId().getValue(),
                c.getFullName(),
                c.getEmail(),
                c.getPhone(),
                c.getDocNumber().getDocType(),
                c.getDocNumber().getValue(),
                c.getValidationStatus().name(),
                c.getStatus().name(),
                c.getCreatedAt()
        );
    }

    private VehicleResult toVehicleResult(Vehicle v) {
        return new VehicleResult(
                v.getId().getValue(),
                v.getTenantId().getValue(),
                v.getCustomerId().getValue(),
                v.getPlate().getValue(),
                v.getBrand(),
                v.getModel(),
                v.getYear(),
                v.getVin(),
                v.getValidationStatus().name(),
                v.getStatus().name(),
                v.getCreatedAt()
        );
    }
}
