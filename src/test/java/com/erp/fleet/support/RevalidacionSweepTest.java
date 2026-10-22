package com.erp.fleet.support;

import com.erp.fleet.application.service.RevalidacionService;
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

import java.sql.Connection;
import java.util.List;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * El barrido revalida los registros pendientes que ya han vencido su espera.
 *
 * <p>Sin el, un registro que se quedo pendiente por una caida del hub
 * necesitaria que alguien lo pidiera a mano, y el fallo del proveedor se
 * convertiria en trabajo manual.
 *
 * <p>El limite por taller es lo que evita que un taller con muchos
 * registros pendientes agote la cuota de validacion del hub, que se comparte
 * entre talleres.
 */
@SpringBootTest
class RevalidacionSweepTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private RevalidacionService revalidacionService;

    @Autowired
    private CustomerRepositoryPort customerRepository;

    @Autowired
    private UnitOfWork unitOfWork;

    @Autowired
    private io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        // El interruptor de circuito es un bean compartido: si otra prueba lo
        // dejo abierto, este barrido no llegaria a consultar al hub. Cada
        // prueba empieza con el circuito cerrado.
        circuitBreakerRegistry.circuitBreaker("hubValidacion").reset();
        TestHubServer.setCaido(false);
        TestHubServer.setObservado(false);
        TestHubServer.reiniciarContador();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Un cliente pendiente vencido se revalida")
    void unClientePendienteVencidoSeRevalida() throws Exception {
        UUID taller = UUID.randomUUID();
        UUID cliente = insertarClientePendiente(taller, "20601030013");

        revalidacionService.ejecutar(10);

        assertEquals("VALID", estadoDe(cliente, taller),
                "El barrido debe revalidar el cliente pendiente");
    }

    @Test
    @DisplayName("Un cliente pendiente que aun no ha vencido no se toca")
    void unClientePendienteQueNoHaVencidoNoSeToca() throws Exception {
        UUID taller = UUID.randomUUID();
        UUID cliente = insertarClientePendiente(taller, "20601030013", 3600);

        revalidacionService.ejecutar(10);

        assertEquals("PENDING", estadoDe(cliente, taller),
                "Un registro que aun no ha vencido no se toca");
    }

    @Test
    @DisplayName("El limite por taller se respeta")
    void elLimitePorTallerSeRespeta() throws Exception {
        UUID taller = UUID.randomUUID();
        UUID primero = insertarClientePendiente(taller, "20601030013");
        UUID segundo = insertarClientePendiente(taller, "20601030014");
        UUID tercero = insertarClientePendiente(taller, "20601030015");

        revalidacionService.ejecutar(1);

        long validados = List.of(primero, segundo, tercero).stream()
                .filter(id -> "VALID".equals(estadoDe(id, taller)))
                .count();

        assertEquals(1, validados,
                "Con un limite de uno por taller, solo se revalida uno de sus registros");
    }

    @Test
    @DisplayName("La revalidacion conserva el taller del registro")
    void laRevalidacionConservaElTallerDelRegistro() throws Exception {
        UUID taller = UUID.randomUUID();
        UUID cliente = insertarClientePendiente(taller, "20601030013");

        revalidacionService.ejecutar(10);

        TenantContextHolder.setTenantId(taller);
        UUID tenantIdDelRegistro = unitOfWork.callInTransaction(() ->
                customerRepository.findById(new CustomerId(cliente), new TenantId(taller))
                        .orElseThrow()
                        .getTenantId()
                        .getValue());
        TenantContextHolder.clear();

        assertEquals(taller, tenantIdDelRegistro,
                "La revalidacion no debe cambiar el taller del registro");
    }

    private UUID insertarClientePendiente(UUID tenantId, String documento) throws Exception {
        return insertarClientePendiente(tenantId, documento, -60);
    }

    private UUID insertarClientePendiente(UUID tenantId, String documento, int segundosEspera)
            throws Exception {
        try (Connection connection = EmbeddedPostgresSupport.openOwnerConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("INSERT INTO customers "
                    + "(tenant_id, full_name, doc_type, doc_number, validation_status, next_validation_at) "
                    + "VALUES ('" + tenantId + "', 'Cliente pendiente', 'RUC', '" + documento + "', "
                    + "'PENDING', now() + interval '" + segundosEspera + " seconds') RETURNING id");
            try (java.sql.ResultSet rs = statement.getResultSet()) {
                rs.next();
                return (UUID) rs.getObject(1);
            }
        }
    }

    private String estadoDe(UUID clienteId, UUID tenantId) {
        TenantContextHolder.setTenantId(tenantId);
        try {
            return unitOfWork.callInTransaction(() -> customerRepository
                    .findById(new CustomerId(clienteId), new TenantId(tenantId))
                    .orElseThrow()
                    .getValidationStatus()
                    .name());
        } finally {
            TenantContextHolder.clear();
        }
    }
}
