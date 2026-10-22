package com.erp.fleet.domain;

import com.erp.fleet.domain.exception.CustomerNotFoundException;
import com.erp.fleet.domain.exception.CustomerWithVehiclesException;
import com.erp.fleet.domain.exception.DocAlreadyExistsException;
import com.erp.fleet.domain.exception.DomainException;
import com.erp.fleet.domain.exception.InvalidPlateException;
import com.erp.fleet.domain.exception.PlateAlreadyExistsException;
import com.erp.fleet.domain.exception.TenantContextMissingException;
import com.erp.fleet.domain.exception.ValidacionNoDisponibleException;
import com.erp.fleet.domain.exception.VehicleNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los errores del dominio llevan un codigo estable.
 *
 * <p>El mensaje se puede reescribir; el codigo no cambia sin que sea un
 * cambio incompatible. Es lo que consume el cliente.
 */
class DomainExceptionTest {

    @Test
    @DisplayName("Cada familia de error tiene su codigo")
    void cadaFamiliaDeErrorTieneSuCodigo() {
        assertEquals("FLEET_DOC_DUPLICADO", new DocAlreadyExistsException("12345678").getCode());
        assertEquals("FLEET_PLATE_DUPLICADO", new PlateAlreadyExistsException("ABC-123").getCode());
        assertEquals("FLEET_CUSTOMER_NOT_FOUND", new CustomerNotFoundException("c-1").getCode());
        assertEquals("FLEET_VEHICLE_NOT_FOUND", new VehicleNotFoundException("v-1").getCode());
        assertEquals("FLEET_CUSTOMER_HAS_VEHICLES", new CustomerWithVehiclesException("c-1").getCode());
        assertEquals("FLEET_TENANT_CONTEXT_MISSING", new TenantContextMissingException().getCode());
        assertEquals("FLEET_VALIDATION_UNAVAILABLE", new ValidacionNoDisponibleException("caido").getCode());
    }

    @Test
    @DisplayName("El error de dominio sin codigo propio tiene el codigo por defecto")
    void elErrorDeDominioSinCodigoPropioTieneElCodigoPorDefecto() {
        assertEquals("FLEET_ERROR", new DomainException("algo").getCode());
        assertEquals("FLEET_ERROR", new InvalidPlateException("placa").getCode());
    }

    @Test
    @DisplayName("El mensaje explica lo ocurrido")
    void elMensajeExplicaLoOcurrido() {
        assertTrue(new DocAlreadyExistsException("20601030013").getMessage().contains("20601030013"));
        assertTrue(new CustomerNotFoundException("c-1").getMessage().contains("c-1"));
        assertTrue(new PlateAlreadyExistsException("ABC-123").getMessage().contains("ABC-123"));
        assertTrue(new CustomerWithVehiclesException("c-1").getMessage().contains("c-1"));
    }
}
