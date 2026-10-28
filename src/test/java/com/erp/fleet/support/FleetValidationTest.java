package com.erp.fleet.support;

import com.erp.fleet.application.dto.command.RegisterCustomerCommand;
import com.erp.fleet.application.dto.command.ValidateCustomerCommand;
import com.erp.fleet.application.dto.result.CustomerResult;
import com.erp.fleet.application.usecase.RegisterCustomerUseCase;
import com.erp.fleet.application.usecase.ValidateCustomerUseCase;
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
 * Integration test verifying customer identity validation through the integration hub.
 */
@SpringBootTest
class FleetValidationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private RegisterCustomerUseCase registerCustomerUseCase;

    @Autowired
    private ValidateCustomerUseCase validateCustomerUseCase;

    @Autowired
    private CustomerRepositoryPort customerRepository;

    @Autowired
    private io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private UnitOfWork unitOfWork;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry.circuitBreaker("hubValidation").reset();
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
    @DisplayName("A customer is registered as PENDING and hub validation confirms it to VALID")
    void customerRegistersPendingAndValidationConfirms() {
        CustomerResult registered = registerCustomerUseCase.register(
                new RegisterCustomerCommand(tenantId, "Talleres SAC", "taller@ejemplo.com",
                        "999999999", "RUC", "20601030013"));

        assertEquals("PENDING", registered.validationStatus());

        CustomerResult validated = validateCustomerUseCase.validate(
                new ValidateCustomerCommand(registered.id(), tenantId));

        assertEquals("VALID", validated.validationStatus());
        assertTrue(TestHubServer.llamadas() >= 1, "Integration hub must have received the request");
    }

    @Test
    @DisplayName("A discrepancy returned by the hub marks customer as OBSERVED")
    void hubDiscrepancyMarksCustomerObserved() {
        TestHubServer.setObservado(true);

        CustomerResult registered = registerCustomerUseCase.register(
                new RegisterCustomerCommand(tenantId, "Talleres SAC", "taller@ejemplo.com",
                        "999999999", "RUC", "20601030013"));

        CustomerResult validated = validateCustomerUseCase.validate(
                new ValidateCustomerCommand(registered.id(), tenantId));

        assertEquals("OBSERVED", validated.validationStatus());
    }

    @Test
    @DisplayName("When hub is down the customer remains PENDING and retry is scheduled")
    void hubDownKeepsCustomerPendingAndSchedulesRetry() {
        TestHubServer.setCaido(true);

        CustomerResult registered = registerCustomerUseCase.register(
                new RegisterCustomerCommand(tenantId, "Talleres SAC", "taller@ejemplo.com",
                        "999999999", "RUC", "20601030013"));

        CustomerResult afterFailure = validateCustomerUseCase.validate(
                new ValidateCustomerCommand(registered.id(), tenantId));

        assertEquals("PENDING", afterFailure.validationStatus());

        Customer saved = unitOfWork.callInTransaction(() -> customerRepository
                .findById(new CustomerId(registered.id()), new TenantId(tenantId))
                .orElseThrow());

        assertNotNull(saved);
        assertEquals("PENDING", saved.getValidationStatus().name());
        assertEquals(1, saved.getValidationAttempts());
        assertNotNull(saved.getNextValidationAt());
    }

    @Test
    @DisplayName("Hub communication failure does not propagate to caller")
    void hubFailureDoesNotPropagateToCaller() {
        TestHubServer.setCaido(true);

        CustomerResult registered = registerCustomerUseCase.register(
                new RegisterCustomerCommand(tenantId, "Talleres SAC", "taller@ejemplo.com",
                        "999999999", "RUC", "20601030013"));

        validateCustomerUseCase.validate(new ValidateCustomerCommand(registered.id(), tenantId));
    }
}
