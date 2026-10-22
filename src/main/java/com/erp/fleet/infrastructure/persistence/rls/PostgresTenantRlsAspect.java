package com.erp.fleet.infrastructure.persistence.rls;

import com.erp.fleet.domain.exception.TenantContextMissingException;
import com.erp.fleet.infrastructure.config.PersistenceConfig;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Establece el contexto de taller en la sesion de base de datos para que la
 * politica de Row-Level Security pueda filtrar las filas.
 *
 * <p>El aislamiento falla cerrado: si no hay transaccion activa, el
 * {@code SET LOCAL} no tendria efecto y la consulta se ejecutaria SIN filtro,
 * de modo que la operacion se rechaza en lugar de ejecutarse sin aislamiento.
 *
 * <p>El contexto lo fija el PRINCIPAL autenticado, nunca la fila que se opera:
 * si lo fijase la fila, la comprobacion de la politica seria siempre
 * satisfactoria y el aislamiento no aislaria nada.
 */
@Aspect
@Component
@Order(PersistenceConfig.TENANT_RLS_ASPECT_ORDER)
public class PostgresTenantRlsAspect {

    private final EntityManager entityManager;

    @Autowired
    public PostgresTenantRlsAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Toda operacion de persistencia debe correr dentro de una transaccion:
     * fuera de ella, el contexto de taller no se puede establecer y la
     * politica de aislamiento no seria aplicable.
     */
    @Before("execution(* com.erp.fleet.infrastructure.persistence.adapter..*(..))")
    public void requireActiveTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new TenantContextMissingException(
                    "La operacion de persistencia debe ejecutarse dentro de una transaccion: "
                    + "fuera de ella no puede garantizarse el aislamiento por taller.");
        }
    }

    /**
     * Para las tablas que pertenecen a un taller, el contexto de taller es
     * obligatorio.
     *
     * <p>El valor se interpola en la sentencia porque {@code SET LOCAL} no
     * admite parametros preparados. El valor es un UUID ya validado, de modo
     * que no introduce una via de inyeccion.
     */
    @Before("execution(* com.erp.fleet.infrastructure.persistence.adapter.CustomerPersistenceAdapter.*(..))"
            + " || execution(* com.erp.fleet.infrastructure.persistence.adapter.VehiclePersistenceAdapter.*(..))")
    public void applyTenantContext() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new TenantContextMissingException(
                    "La operacion requiere un contexto de taller establecido y no lo tiene.");
        }
        entityManager.createNativeQuery("SET LOCAL app.tenant_id = '" + tenantId + "'").executeUpdate();
    }
}
