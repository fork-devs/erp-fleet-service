package com.erp.fleet.domain;

import com.erp.fleet.domain.exception.InvalidPlateException;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.DocNumber;
import com.erp.fleet.domain.valueobject.Plate;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.domain.valueobject.VehicleId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los value objects del dominio.
 *
 * <p>Son la frontera de la entrada del usuario: normalizan lo que llega y
 * rechazan lo que no tiene sentido antes de que llegue a la base de datos.
 */
class ValueObjectsTest {

    @Test
    @DisplayName("La placa se normaliza a mayusculas")
    void laPlacaSeNormalizaAMayusculas() {
        assertEquals("ABC-123", Plate.of("abc-123").getValue());
        assertEquals("ABC-123", Plate.of("  ABC-123  ").getValue());
    }

    @Test
    @DisplayName("La placa acepta el formato peruano y rechaza el demas")
    void laPlacaAceptaElFormatoPeruanoYRechazaElDemas() {
        assertTrue(Plate.of("ABC-1234").getValue().endsWith("1234"));

        assertThrows(InvalidPlateException.class, () -> Plate.of("NO-VALIDA"));
        assertThrows(InvalidPlateException.class, () -> Plate.of("ABCD-123"));
        assertThrows(InvalidPlateException.class, () -> Plate.of("AB-123"));
        assertThrows(InvalidPlateException.class, () -> Plate.of("ABC-12"));
        assertThrows(InvalidPlateException.class, () -> Plate.of(null));
        assertThrows(InvalidPlateException.class, () -> Plate.of("   "));
    }

    @Test
    @DisplayName("El documento conserva su tipo y su numero")
    void elDocumentoConservaSuTipoYSuNumero() {
        DocNumber documento = DocNumber.of(" 20601030013 ", "RUC");

        assertEquals("20601030013", documento.getValue());
        assertEquals("RUC", documento.getDocType());
        assertEquals("RUC:20601030013", documento.toString());
    }

    @Test
    @DisplayName("El documento no puede estar vacio")
    void elDocumentoNoPuedeEstarVacio() {
        assertThrows(IllegalArgumentException.class, () -> DocNumber.of("", "DNI"));
        assertThrows(IllegalArgumentException.class, () -> DocNumber.of(null, "DNI"));
    }

    @Test
    @DisplayName("Dos documentos con el mismo tipo y numero son el mismo")
    void dosDocumentosConElMismoTipoYNumeroSonElMismo() {
        assertEquals(DocNumber.of("12345678", "DNI"), DocNumber.of("12345678", "DNI"));
        assertEquals(DocNumber.of("12345678", "DNI").hashCode(),
                DocNumber.of("12345678", "DNI").hashCode());
        assertNotEquals(DocNumber.of("12345678", "DNI"), DocNumber.of("12345678", "RUC"));
    }

    @Test
    @DisplayName("El identificador de taller no puede ser nulo")
    void elIdentificadorDeTallerNoPuedeSerNulo() {
        assertThrows(IllegalArgumentException.class, () -> new TenantId(null));

        UUID tenant = UUID.randomUUID();
        assertEquals(tenant, new TenantId(tenant).getValue());
    }

    @Test
    @DisplayName("Los identificadores de cliente y de vehiculo se generan y se comparan")
    void losIdentificadoresDeClienteYDeVehiculoSeGeneranYSeComparan() {
        CustomerId cliente = CustomerId.generate();
        VehicleId vehiculo = VehicleId.generate();

        assertEquals(cliente, cliente);
        assertNotEquals(cliente, CustomerId.generate());
        assertEquals(vehiculo, vehiculo);
        assertNotEquals(cliente.getValue(), vehiculo.getValue());
    }

    @Test
    @DisplayName("El identificador de cliente se puede reconstruir desde su texto")
    void elIdentificadorDeClienteSePuedeReconstruirDesdeSuTexto() {
        CustomerId cliente = CustomerId.generate();

        assertEquals(cliente, CustomerId.fromString(cliente.toString()));
        assertEquals(cliente.getValue().toString(), cliente.toString());
        assertEquals(cliente, CustomerId.fromString(cliente.getValue().toString()));
        assertNotEquals(cliente, CustomerId.fromString(UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName("El identificador de vehiculo se puede reconstruir desde su texto")
    void elIdentificadorDeVehiculoSePuedeReconstruirDesdeSuTexto() {
        VehicleId vehiculo = VehicleId.generate();

        assertEquals(vehiculo, VehicleId.fromString(vehiculo.toString()));
        assertEquals(vehiculo.getValue().toString(), vehiculo.toString());
        assertNotEquals(vehiculo, VehicleId.fromString(UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName("Los identificadores no pueden ser nulos")
    void losIdentificadoresNoPuedenSerNulos() {
        assertThrows(IllegalArgumentException.class, () -> new CustomerId(null));
        assertThrows(IllegalArgumentException.class, () -> new VehicleId(null));
    }

    @Test
    @DisplayName("El identificador de taller se puede reconstruir desde su texto")
    void elIdentificadorDeTallerSePuedeReconstruirDesdeSuTexto() {
        TenantId tenant = TenantId.fromString(UUID.randomUUID().toString());

        assertEquals(tenant, TenantId.fromString(tenant.getValue().toString()));
        assertEquals(tenant.getValue().toString(), tenant.toString());
        assertThrows(IllegalArgumentException.class, () -> TenantId.fromString("no-es-un-uuid"));
        assertThrows(IllegalArgumentException.class, () -> TenantId.fromString("  "));
    }
}
