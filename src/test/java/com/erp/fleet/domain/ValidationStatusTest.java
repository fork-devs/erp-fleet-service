package com.erp.fleet.domain;

import com.erp.fleet.domain.model.Customer;
import com.erp.fleet.domain.model.ValidationStatus;
import com.erp.fleet.domain.model.Vehicle;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.DocNumber;
import com.erp.fleet.domain.valueobject.Plate;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.domain.valueobject.VehicleId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * El estado de la validacion distingue tres situaciones y no dos.
 *
 * <p>Con un estado {@code FAILED} no se puede distinguir la discrepancia que
 * informa el proveedor, que hay que mostrar al taller, del fallo del
 * proveedor, que hay que reintentar. Con tres estados, cada situacion tiene su
 * tratamiento.
 */
class ValidationStatusTest {

    private static final TenantId TALLER = new TenantId(UUID.randomUUID());

    @Test
    @DisplayName("Un cliente recien creado esta pendiente de validar")
    void unClienteRecienCreadoEstaPendiente() {
        Customer cliente = cliente();

        assertEquals(ValidationStatus.PENDING, cliente.getValidationStatus());
    }

    @Test
    @DisplayName("Un cliente validado guarda lo que informa el hub")
    void unClienteValidadoGuardaLoQueInformaElHub() {
        Customer cliente = cliente();

        cliente.markValidated("Talleres SAC", "ACTIVO", "HABIDO", "Av. Siempre Viva 742", Instant.now());

        assertEquals(ValidationStatus.VALID, cliente.getValidationStatus());
        assertEquals("Talleres SAC", cliente.getRazonSocial());
        assertEquals("HABIDO", cliente.getCondicionRuc());
        assertEquals("Av. Siempre Viva 742", cliente.getDireccionFiscal());
    }

    @Test
    @DisplayName("Un cliente con discrepancia queda observado, no fallido")
    void unClienteConDiscrepanciaQuedaObservado() {
        Customer cliente = cliente();

        cliente.markObserved();

        assertEquals(ValidationStatus.OBSERVED, cliente.getValidationStatus());
        assertEquals(ValidationStatus.OBSERVED.name(), cliente.getValidationStatus().name());
    }

    @Test
    @DisplayName("Un vehiculo con discrepancia queda observado y conserva sus datos")
    void unVehiculoConDiscrepanciaQuedaObservado() {
        Vehicle vehiculo = vehiculo();

        vehiculo.markObserved();

        assertEquals(ValidationStatus.OBSERVED, vehiculo.getValidationStatus());
        assertEquals("ABC-123", vehiculo.getPlate().getValue());
    }

    @Test
    @DisplayName("Un intento fallido se cuenta y se programa el siguiente")
    void unIntentoFallidoSeCuentaYSeProgramaElSiguiente() {
        Customer cliente = cliente();
        Instant siguiente = Instant.now().plusSeconds(300);

        cliente.registerValidationAttempt(siguiente);
        cliente.registerValidationAttempt(siguiente);

        assertEquals(2, cliente.getValidationAttempts());
        assertEquals(siguiente, cliente.getNextValidationAt());
        assertEquals(ValidationStatus.PENDING, cliente.getValidationStatus(),
                "Un intento fallido no cambia el estado de la validacion");
    }

    private Customer cliente() {
        return Customer.create(TALLER, "Juan Perez", "juan@ejemplo.com", "999999999",
                DocNumber.of("12345678", "DNI"));
    }

    private Vehicle vehiculo() {
        return Vehicle.create(TALLER, new CustomerId(UUID.randomUUID()), Plate.of("ABC-123"),
                "Toyota", "Corolla", 2021, null);
    }
}
