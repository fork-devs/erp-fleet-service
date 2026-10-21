package com.erp.fleet.support;

import com.erp.fleet.application.dto.command.RegisterCustomerCommand;
import com.erp.fleet.application.dto.command.RegisterVehicleCommand;
import com.erp.fleet.application.dto.command.UpdateCustomerCommand;
import com.erp.fleet.application.dto.command.ValidarClienteCommand;
import com.erp.fleet.application.dto.result.CustomerResult;
import com.erp.fleet.application.usecase.RegisterCustomerUseCase;
import com.erp.fleet.application.usecase.UpdateCustomerUseCase;
import com.erp.fleet.application.usecase.ValidarClienteUseCase;
import com.erp.fleet.domain.port.CustomerRepositoryPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.infrastructure.persistence.rls.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Aislamiento entre talleres.
 *
 * <p>El atacante es el ADMIN_TALLER del taller A y la victima es el taller B.
 * Los cuatro vectores: leer, escribir, validar y dar de baja.
 *
 * <p>El aislamiento no depende de que cada consulta recuerde filtrar por
 * taller: lo impone la politica de Row-Level Security, y el contexto lo fija el
 * principal autenticado.
 */
@SpringBootTest
class CrossTenantAccessTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private RegisterCustomerUseCase registerCustomerUseCase;

    @Autowired
    private UpdateCustomerUseCase updateCustomerUseCase;

    @Autowired
    private ValidarClienteUseCase validarClienteUseCase;

    @Autowired
    private CustomerRepositoryPort customerRepository;

    @Autowired
    private UnitOfWork unitOfWork;

    @Autowired
    private com.erp.fleet.application.usecase.RegisterVehicleUseCase registerVehicleUseCase;

    private UUID tallerA;
    private UUID tallerB;

    @BeforeEach
    void setUp() {
        tallerA = UUID.randomUUID();
        tallerB = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("El taller A no lee los clientes del taller B")
    void elTallerANoLeeLosClientesDelTallerB() {
        CustomerResult delOtroTaller = clienteDelTaller(tallerB);

        TenantContextHolder.setTenantId(tallerA);
        // Los adaptadores se unen a la transaccion que abre la capa de
        // aplicacion: la lectura va dentro de ella.
        boolean encontrado = unitOfWork.callInTransaction(() ->
                customerRepository.findById(new CustomerId(delOtroTaller.id()), new TenantId(tallerA))
                        .isPresent());

        assertTrue(!encontrado,
                "La politica de aislamiento no puede devolver un cliente de otro taller");
    }

    @Test
    @DisplayName("El taller A no actualiza un cliente del taller B")
    void elTallerANoActualizaUnClienteDelTallerB() {
        CustomerResult delOtroTaller = clienteDelTaller(tallerB);

        TenantContextHolder.setTenantId(tallerA);
        assertThrows(RuntimeException.class, () -> updateCustomerUseCase.update(
                        new UpdateCustomerCommand(delOtroTaller.id(), tallerA, "Intruso", null, null)),
                "Actualizar un cliente de otro taller tiene que rechazarse");
    }

    @Test
    @DisplayName("El taller A no valida un cliente del taller B")
    void elTallerANoValidaUnClienteDelTallerB() {
        CustomerResult delOtroTaller = clienteDelTaller(tallerB);

        TenantContextHolder.setTenantId(tallerA);
        assertThrows(RuntimeException.class, () -> validarClienteUseCase.validar(
                        new ValidarClienteCommand(delOtroTaller.id(), tallerA)),
                "Validar un cliente de otro taller tiene que rechazarse");
    }

    @Test
    @DisplayName("El taller A no cuelga un vehiculo de un cliente del taller B")
    void elTallerANoCuelgaUnVehiculoDeUnClienteDelTallerB() {
        CustomerResult delOtroTaller = clienteDelTaller(tallerB);

        TenantContextHolder.setTenantId(tallerA);
        assertThrows(RuntimeException.class, () -> registerVehicleUseCase.register(
                        new RegisterVehicleCommand(tallerA, delOtroTaller.id(), "ABC-123",
                                "Toyota", "Corolla", 2021, null)),
                "Un vehiculo no puede colgar de un cliente de otro taller");
    }

    @Test
    @DisplayName("El listado de un taller no incluye los clientes del otro")
    void elListadoDeUnTallerNoIncluyeLosClientesDelOtro() {
        clienteDelTaller(tallerA);
        clienteDelTaller(tallerB);

        TenantContextHolder.setTenantId(tallerA);
        List<?> clientes = unitOfWork.callInTransaction(() ->
                customerRepository.findAllByTenantId(new TenantId(tallerA)));

        assertEquals(1, clientes.size(),
                "El listado debe contener solo los clientes del taller del principal");
    }

    private CustomerResult clienteDelTaller(UUID tenantId) {
        TenantContextHolder.setTenantId(tenantId);
        try {
            return registerCustomerUseCase.register(new RegisterCustomerCommand(tenantId,
                    "Cliente del taller", "cliente@ejemplo.com", "999999999", "RUC",
                    String.valueOf(Math.abs(UUID.randomUUID().hashCode() % 1000000000)) + "13"));
        } finally {
            TenantContextHolder.clear();
        }
    }
}
