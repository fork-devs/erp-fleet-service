package com.erp.fleet.application.service;

import com.erp.fleet.application.dto.command.ValidarClienteCommand;
import com.erp.fleet.application.dto.command.ValidarVehiculoCommand;
import com.erp.fleet.application.dto.result.CustomerResult;
import com.erp.fleet.application.dto.result.VehicleResult;
import com.erp.fleet.application.usecase.ReintentarValidacionUseCase;
import com.erp.fleet.application.usecase.ValidarClienteUseCase;
import com.erp.fleet.application.usecase.ValidarVehiculoUseCase;
import com.erp.fleet.domain.exception.CustomerNotFoundException;
import com.erp.fleet.domain.exception.VehicleNotFoundException;
import com.erp.fleet.domain.model.Customer;
import com.erp.fleet.domain.model.ResultadoValidacionIdentidad;
import com.erp.fleet.domain.model.ResultadoValidacionVehiculo;
import com.erp.fleet.domain.model.ValidationStatus;
import com.erp.fleet.domain.model.Vehicle;
import com.erp.fleet.domain.port.CustomerRepositoryPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.port.ValidacionCachePort;
import com.erp.fleet.domain.port.ValidacionIdentidadPort;
import com.erp.fleet.domain.port.ValidacionVehiculoPort;
import com.erp.fleet.domain.port.VehicleRepositoryPort;
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
 * Validacion de clientes y de vehiculos contra el hub de integraciones.
 *
 * <p>La validacion NO bloquea al taller: el registro se confirma antes, con
 * estado pendiente, y la validacion viene despues. Si el hub no esta
 * disponible, el registro sigue pendiente, se cuenta el intento y se programa
 * el siguiente: no se pierde nada y el operador no recibe un error.
 *
 * <p>El interruptor de circuito vive aqui, en quien llama, porque el
 * degradado —dejar el registro pendiente— es una decision de este servicio:
 * un circuito dentro del hub solo podria negarse a responder, no comunicar
 * al que llama que debe degradar.
 *
 * <p>El resultado correcto se sirve con cache: un RUC durante treinta dias y
 * una placa durante siete. Un resultado nuevo del hub invalida la entrada
 * local.
 */
public class ValidacionService implements
        ValidarClienteUseCase, ValidarVehiculoUseCase, ReintentarValidacionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ValidacionService.class);

    /** Espera inicial entre reintentos. */
    private static final Duration ESPERA_INICIAL = Duration.ofMinutes(1);

    /** Espera maxima entre reintentos: la ventana de 24 horas del diseno. */
    private static final Duration ESPERA_MAXIMA = Duration.ofHours(24);

    private final CustomerRepositoryPort customerRepository;
    private final VehicleRepositoryPort vehicleRepository;
    private final ValidacionIdentidadPort validacionIdentidad;
    private final ValidacionVehiculoPort validacionVehiculo;
    private final ValidacionCachePort cache;
    private final UnitOfWork unitOfWork;

    public ValidacionService(CustomerRepositoryPort customerRepository,
                              VehicleRepositoryPort vehicleRepository,
                              ValidacionIdentidadPort validacionIdentidad,
                              ValidacionVehiculoPort validacionVehiculo,
                              ValidacionCachePort cache,
                              UnitOfWork unitOfWork) {
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.validacionIdentidad = validacionIdentidad;
        this.validacionVehiculo = validacionVehiculo;
        this.cache = cache;
        this.unitOfWork = unitOfWork;
    }

    @Override
    @CircuitBreaker(name = "hubValidacion", fallbackMethod = "validarClienteDegradado")
    public CustomerResult validar(ValidarClienteCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            Customer customer = customerRepository
                    .findById(new CustomerId(cmd.customerId()), new TenantId(cmd.tenantId()))
                    .orElseThrow(() -> new CustomerNotFoundException(cmd.customerId().toString()));

            String documento = customer.getDocNumber().getValue();
            Optional<ResultadoValidacionIdentidad> enCache = cache.leerRuc(documento);

            ResultadoValidacionIdentidad resultado = enCache.orElseGet(() -> {
                ResultadoValidacionIdentidad consultado = validacionIdentidad.validarDocumento(documento);
                cache.guardarRuc(documento, consultado);
                return consultado;
            });

            if (resultado.esValido()) {
                customer.markValidated(resultado.getRazonSocial(), resultado.getEstado(),
                        resultado.getCondicion(), resultado.getDireccionFiscal(), Instant.now());
            } else {
                customer.markObserved();
            }

            return toCustomerResult(customerRepository.save(customer));
        });
    }

    /**
     * Degradado: el hub no esta disponible. El registro sigue pendiente, se
     * cuenta el intento y se programa el siguiente. No se propaga el fallo.
     */
    @SuppressWarnings("unused")
    private CustomerResult validarClienteDegradado(ValidarClienteCommand cmd, Throwable causa) {
        log.warn("La validacion del cliente {} se degrada a pendiente: {}",
                cmd.customerId(), causa.getMessage());
        return unitOfWork.callInTransaction(() -> {
            Customer customer = customerRepository
                    .findById(new CustomerId(cmd.customerId()), new TenantId(cmd.tenantId()))
                    .orElseThrow(() -> new CustomerNotFoundException(cmd.customerId().toString()));
            customer.registerValidationAttempt(proximoIntento(customer.getValidationAttempts()));
            return toCustomerResult(customerRepository.save(customer));
        });
    }

    @Override
    @CircuitBreaker(name = "hubValidacion", fallbackMethod = "validarVehiculoDegradado")
    public VehicleResult validar(ValidarVehiculoCommand cmd) {
        return unitOfWork.callInTransaction(() -> {
            Vehicle vehicle = vehicleRepository
                    .findById(new VehicleId(cmd.vehicleId()), new TenantId(cmd.tenantId()))
                    .orElseThrow(() -> new VehicleNotFoundException(cmd.vehicleId().toString()));

            String placa = vehicle.getPlate().getValue();
            Optional<ResultadoValidacionVehiculo> enCache = cache.leerPlaca(placa);

            ResultadoValidacionVehiculo resultado = enCache.orElseGet(() -> {
                ResultadoValidacionVehiculo consultado = validacionVehiculo.validarPlaca(placa);
                cache.guardarPlaca(placa, consultado);
                return consultado;
            });

            if (resultado.esValido()) {
                vehicle.markValidated(resultado.getVin(), resultado.getNumeroMotor(),
                        resultado.getMarca(), resultado.getModelo(), resultado.getColor(),
                        resultado.getCombustible(), Instant.now());
            } else {
                vehicle.markObserved();
            }

            return toVehicleResult(vehicleRepository.save(vehicle));
        });
    }

    /**
     * Degradado: el hub no esta disponible. El vehiculo sigue pendiente, se
     * cuenta el intento y se programa el siguiente.
     */
    @SuppressWarnings("unused")
    private VehicleResult validarVehiculoDegradado(ValidarVehiculoCommand cmd, Throwable causa) {
        log.warn("La validacion del vehiculo {} se degrada a pendiente: {}",
                cmd.vehicleId(), causa.getMessage());
        return unitOfWork.callInTransaction(() -> {
            Vehicle vehicle = vehicleRepository
                    .findById(new VehicleId(cmd.vehicleId()), new TenantId(cmd.tenantId()))
                    .orElseThrow(() -> new VehicleNotFoundException(cmd.vehicleId().toString()));
            vehicle.registerValidationAttempt(proximoIntento(vehicle.getValidationAttempts()));
            return toVehicleResult(vehicleRepository.save(vehicle));
        });
    }

    /**
     * Un registro ya validado u observado NO se vuelve a consultar: no hay
     * nada pendiente de validar, y repetir la consulta seria gastar la cuota
     * del taller sin motivo.
     */
    @Override
    public boolean reintentarCliente(UUID customerId, UUID tenantId) {
        boolean pendiente = unitOfWork.callInTransaction(() -> {
            Customer customer = customerRepository
                    .findById(new CustomerId(customerId), new TenantId(tenantId))
                    .orElseThrow(() -> new CustomerNotFoundException(customerId.toString()));
            return customer.getValidationStatus() == ValidationStatus.PENDING;
        });

        if (!pendiente) {
            return false;
        }

        validar(new ValidarClienteCommand(customerId, tenantId));
        return true;
    }

    @Override
    public boolean reintentarVehiculo(UUID vehicleId, UUID tenantId) {
        boolean pendiente = unitOfWork.callInTransaction(() -> {
            Vehicle vehicle = vehicleRepository
                    .findById(new VehicleId(vehicleId), new TenantId(tenantId))
                    .orElseThrow(() -> new VehicleNotFoundException(vehicleId.toString()));
            return vehicle.getValidationStatus() == ValidationStatus.PENDING;
        });

        if (!pendiente) {
            return false;
        }

        validar(new ValidarVehiculoCommand(vehicleId, tenantId));
        return true;
    }

    /**
     * Espera creciente entre reintentos, con la ventana maxima del diseno.
     */
    private Instant proximoIntento(int intentos) {
        int exponente = Math.min(Math.max(intentos, 0), 16);
        long espera = Math.min(ESPERA_INICIAL.toSeconds() * (1L << exponente),
                ESPERA_MAXIMA.toSeconds());
        return Instant.now().plusSeconds(espera);
    }

    private CustomerResult toCustomerResult(Customer c) {
        return new CustomerResult(
                c.getId().getValue(), c.getTenantId().getValue(),
                c.getFullName(), c.getEmail(), c.getPhone(),
                c.getDocNumber().getDocType(), c.getDocNumber().getValue(),
                c.getValidationStatus().name(), c.getStatus().name(), c.getCreatedAt()
        );
    }

    private VehicleResult toVehicleResult(Vehicle v) {
        return new VehicleResult(
                v.getId().getValue(), v.getTenantId().getValue(), v.getCustomerId().getValue(),
                v.getPlate().getValue(), v.getBrand(), v.getModel(), v.getYear(), v.getVin(),
                v.getValidationStatus().name(), v.getStatus().name(), v.getCreatedAt()
        );
    }
}
