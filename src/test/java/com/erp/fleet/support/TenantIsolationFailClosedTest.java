package com.erp.fleet.support;

import com.erp.fleet.domain.exception.TenantContextMissingException;
import com.erp.fleet.domain.port.CustomerRepositoryPort;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.infrastructure.persistence.rls.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * El aislamiento falla cerrado.
 *
 * <p>Una operacion que necesita contexto de taller y no lo tiene se rechaza de
 * forma explicita, en lugar de ejecutarse sin filtrar. Sin este cierre, la
 * politica de Row-Level Security se relaja en silencio justo en el camino que
 * no deberia ocurrir: el que no ha pasado por el filtro de autenticacion.
 */
class TenantIsolationFailClosedTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private CustomerRepositoryPort customerRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void limpiar() {
        TenantContextHolder.clear();
    }

    @Test
    void unaOperacionSinContextoDeTallerSeRechaza() {
        TenantContextMissingException error = assertThrows(TenantContextMissingException.class,
                () -> transactionTemplate.executeWithoutResult(status ->
                        customerRepository.findAllByTenantId(new TenantId(UUID.randomUUID()))));

        assertEquals("FLEET_TENANT_CONTEXT_MISSING", error.getCode());
    }

    @Test
    void unaOperacionFueraDeLaTransaccionSeRechaza() {
        assertThrows(TenantContextMissingException.class,
                () -> customerRepository.findAllByTenantId(new TenantId(UUID.randomUUID())));
    }

    @Test
    void conContextoDeTallerLaOperacionSeEjecuta() {
        UUID taller = UUID.randomUUID();

        List<?> clientes = transactionTemplate.execute(status -> {
            TenantContextHolder.setTenantId(taller);
            try {
                return customerRepository.findAllByTenantId(new TenantId(taller));
            } finally {
                TenantContextHolder.clear();
            }
        });

        assertEquals(0, clientes.size(),
                "El taller no tiene clientes: la consulta se ejecuta y devuelve los suyos");
    }
}
