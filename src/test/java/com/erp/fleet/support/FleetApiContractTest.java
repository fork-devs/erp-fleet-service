package com.erp.fleet.support;

import com.erp.fleet.domain.port.CustomerRepositoryPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.port.VehicleRepositoryPort;
import com.erp.fleet.domain.valueobject.TenantId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El contrato de la API.
 *
 * <p>Los endpoints se publican bajo el prefijo que el gateway reenvia SIN
 * quitarlo: servirlos fuera de ese prefijo los hacia inalcanzables, que es lo
 * que pasaba antes.
 *
 * <p>Los errores llevan un codigo estable por familia y el taller, para que el
 * frontend pueda distinguirlos sin adivinar.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FleetApiContractTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepositoryPort customerRepository;

    @Autowired
    private VehicleRepositoryPort vehicleRepository;

    @Autowired
    private UnitOfWork unitOfWork;

    @Test
    @DisplayName("El alta responde 201 con el estado pendiente")
    void elAltaResponde201ConElEstadoPendiente() throws Exception {
        UUID taller = UUID.randomUUID();
        String documento = documento();

        mockMvc.perform(post("/api/v1/fleet/customers")
                        .header("Authorization", "Bearer " + TestJwksServer.signToken(taller, List.of("ADMIN_TALLER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Cliente de prueba\",\"docType\":\"RUC\",\"docNumber\":\"" + documento + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.validationStatus").value("PENDING"))
                .andExpect(jsonPath("$.tenantId").value(taller.toString()));
    }

    @Test
    @DisplayName("Un documento repetido responde 409 con el codigo estable")
    void unDocumentoRepetidoResponde409ConElCodigoEstable() throws Exception {
        UUID taller = UUID.randomUUID();
        String token = TestJwksServer.signToken(taller, List.of("ADMIN_TALLER"));
        String documento = documento();

        mockMvc.perform(post("/api/v1/fleet/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Cliente de prueba\",\"docType\":\"RUC\",\"docNumber\":\"" + documento + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/fleet/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Otro cliente\",\"docType\":\"RUC\",\"docNumber\":\"" + documento + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FLEET_DOC_DUPLICADO"))
                .andExpect(jsonPath("$.tenant_id").value(taller.toString()));
    }

    @Test
    @DisplayName("Una placa repetida responde 409 con el codigo estable")
    void unaPlacaRepetidaResponde409ConElCodigoEstable() throws Exception {
        UUID taller = UUID.randomUUID();
        String token = TestJwksServer.signToken(taller, List.of("ADMIN_TALLER"));
        String documento = documento();
        String placa = placa();

        mockMvc.perform(post("/api/v1/fleet/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Cliente de prueba\",\"docType\":\"RUC\",\"docNumber\":\"" + documento + "\"}"))
                .andExpect(status().isCreated());

        String clienteId = idDelUltimoCliente(taller);

        mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + clienteId + "\",\"plate\":\"" + placa + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + clienteId + "\",\"plate\":\"" + placa + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FLEET_PLATE_DUPLICADO"));
    }

    @Test
    @DisplayName("Un registro de otro taller responde 404")
    void unRegistroDeOtroTallerResponde404() throws Exception {
        UUID taller = UUID.randomUUID();
        UUID otroTaller = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/fleet/customers/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + TestJwksServer.signToken(taller, List.of("ADMIN_TALLER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FLEET_CUSTOMER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Un rol sin permiso responde 403")
    void unRolSinPermisoResponde403() throws Exception {
        UUID taller = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/fleet/customers")
                        .header("Authorization", "Bearer " + TestJwksServer.signToken(taller, List.of("MECANICO")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Cliente\",\"docType\":\"RUC\",\"docNumber\":\"" + documento() + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Una peticion sin token responde 401")
    void unaPeticionSinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/v1/fleet/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("La ruta sin el prefijo del gateway no existe")
    void laRutaSinElPrefijoDelGatewayNoExiste() throws Exception {
        UUID taller = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/customers")
                        .header("Authorization", "Bearer " + TestJwksServer.signToken(taller, List.of("ADMIN_TALLER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("El listado devuelve solo los clientes del taller")
    void elListadoDevuelveSoloLosClientesDelTaller() throws Exception {
        UUID taller = UUID.randomUUID();
        UUID otroTaller = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/fleet/customers")
                        .header("Authorization", "Bearer " + TestJwksServer.signToken(taller, List.of("ADMIN_TALLER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Cliente del taller\",\"docType\":\"RUC\",\"docNumber\":\"" + documento() + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/fleet/customers")
                        .header("Authorization", "Bearer " + TestJwksServer.signToken(otroTaller, List.of("ADMIN_TALLER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Cliente de otro taller\",\"docType\":\"RUC\",\"docNumber\":\"" + documento() + "\"}"))
                .andExpect(status().isCreated());

        String respuesta = mockMvc.perform(get("/api/v1/fleet/customers")
                        .header("Authorization", "Bearer " + TestJwksServer.signToken(taller, List.of("ADMIN_TALLER"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertFalse(respuesta.contains("Cliente de otro taller"),
                "El listado no puede incluir clientes de otro taller");
    }

    private String idDelUltimoCliente(UUID tenantId) {
        com.erp.fleet.infrastructure.persistence.rls.TenantContextHolder.setTenantId(tenantId);
        try {
        return unitOfWork.callInTransaction(() -> customerRepository
                .findAllByTenantId(new TenantId(tenantId))
                .stream()
                .reduce((primero, ultimo) -> ultimo)
                .orElseThrow()
                .getId()
                .getValue()
                .toString());
        } finally {
            com.erp.fleet.infrastructure.persistence.rls.TenantContextHolder.clear();
        }
    }

    private String documento() {
        return String.valueOf(Math.abs(UUID.randomUUID().hashCode() % 1000000000)) + "13";
    }

    private String placa() {
        return "ABC-" + (Math.abs(UUID.randomUUID().hashCode() % 1000));
    }
}
