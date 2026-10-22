package com.erp.fleet.support;

import com.erp.fleet.application.dto.command.RegisterCustomerCommand;
import com.erp.fleet.application.dto.command.ValidarClienteCommand;
import com.erp.fleet.application.dto.result.CustomerResult;
import com.erp.fleet.application.usecase.RegisterCustomerUseCase;
import com.erp.fleet.application.usecase.ValidarClienteUseCase;
import com.erp.fleet.domain.model.Customer;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * La validacion contra el hub existe, y su caida no pierde el registro.
 *
 * <p>El hub es el unico punto de salida a RENIEC, SUNAT y la consulta
 * vehicular. La documentacion del servicio describia el flujo de validacion
 * asincrona, pero no habia ni cliente, ni cache, ni interruptor de circuito:
 * el registro se quedaba en PENDING para siempre.
 */
@SpringBootTest
class FleetValidationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private RegisterCustomerUseCase registerCustomerUseCase;

    @Autowired
    private ValidarClienteUseCase validarClienteUseCase;

    @Autowired
    private CustomerRepositoryPort customerRepository;

    @Autowired
    private io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private UnitOfWork unitOfWork;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        // El interruptor de circuito es un bean compartido: cada prueba
        // empieza con el circuito cerrado.
        circuitBreakerRegistry.circuitBreaker("hubValidacion").reset();
        tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);
        TestHubServer.setCaido(false);
        TestHubServer.setObservado(false);
        TestHubServer.reiniciarContador();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Un cliente se registra pendiente y la validacion lo confirma")
    void unClienteSeRegistraPendienteYLaValidacionLoConfirma() {
        CustomerResult registrado = registerCustomerUseCase.register(
                new RegisterCustomerCommand(tenantId, "Talleres SAC", "taller@ejemplo.com",
                        "999999999", "RUC", "20601030013"));

        assertEquals("PENDING", registrado.validationStatus());

        CustomerResult validado = validarClienteUseCase.validar(
                new ValidarClienteCommand(registrado.id(), tenantId));

        assertEquals("VALID", validado.validationStatus());
        assertTrue(TestHubServer.llamadas() >= 1, "El hub debe haber recibido la consulta");
    }

    @Test
    @DisplayName("Una discrepancia del hub deja el cliente observado")
    void unaDiscrepanciaDelHubDejaElClienteObservado() {
        TestHubServer.setObservado(true);

        CustomerResult registrado = registerCustomerUseCase.register(
                new RegisterCustomerCommand(tenantId, "Talleres SAC", "taller@ejemplo.com",
                        "999999999", "RUC", "20601030013"));

        CustomerResult validado = validarClienteUseCase.validar(
                new ValidarClienteCommand(registrado.id(), tenantId));

        assertEquals("OBSERVED", validado.validationStatus());
    }

    @Test
    @DisplayName("Con el hub caido el registro no se pierde y queda pendiente")
    void conElHubCaidoElRegistroNoSePierdeYQuedaPendiente() {
        TestHubServer.setCaido(true);

        CustomerResult registrado = registerCustomerUseCase.register(
                new RegisterCustomerCommand(tenantId, "Talleres SAC", "taller@ejemplo.com",
                        "999999999", "RUC", "20601030013"));

        CustomerResult trasElFallo = validarClienteUseCase.validar(
                new ValidarClienteCommand(registrado.id(), tenantId));

        assertEquals("PENDING", trasElFallo.validationStatus(),
                "Con el hub caido el registro sigue pendiente: no se marca como no valido");

        // Los adaptadores se unen a la transaccion que abre la capa de
        // aplicacion: una lectura directa fuera de ella no es valida.
        Customer guardado = unitOfWork.callInTransaction(() -> customerRepository
                .findById(new CustomerId(registrado.id()), new TenantId(tenantId))
                .orElseThrow());

        assertNotNull(guardado);
        assertEquals("PENDING", guardado.getValidationStatus().name());
        assertEquals(1, guardado.getValidationAttempts(),
                "El intento fallido se cuenta y el siguiente se programa");
        assertNotNull(guardado.getNextValidationAt(),
                "El fallo programa el siguiente intento");
    }

    @Test
    @DisplayName("El fallo del hub no se propaga al operador")
    void elFalloDelHubNoSePropagaAlOperador() {
        TestHubServer.setCaido(true);

        CustomerResult registrado = registerCustomerUseCase.register(
                new RegisterCustomerCommand(tenantId, "Talleres SAC", "taller@ejemplo.com",
                        "999999999", "RUC", "20601030013"));

        validarClienteUseCase.validar(new ValidarClienteCommand(registrado.id(), tenantId));
        // Si la excepcion se propagase, el test fallaria aqui.
    }
}
