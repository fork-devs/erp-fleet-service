package com.erp.fleet.support;

import com.erp.fleet.domain.port.CustomerRepositoryPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.infrastructure.persistence.rls.TenantContextHolder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La baja de un cliente es LOGICA y tiene reglas.
 *
 * <p>El registro se conserva: la baja no es un borrado. Y un cliente con
 * vehiculos asociados no se puede dar de baja: sus vehiculos seguirian
 * apuntando a un cliente dado de baja, con la orden de trabajo de un taller
 * sin propietario.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FleetDeactivationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepositoryPort customerRepository;

    @Autowired
    private UnitOfWork unitOfWork;

    @Test
    @DisplayName("Un cliente sin vehiculos se da de baja y su registro se conserva")
    void unClienteSinVehiculosSeDaDeBajaYSuRegistroSeConserva() throws Exception {
        UUID taller = UUID.randomUUID();
        String token = TestJwksServer.signToken(taller, List.of("ADMIN_TALLER"));
        String documento = documento();

        mockMvc.perform(post("/api/v1/fleet/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Cliente sin vehiculos\",\"docType\":\"RUC\",\"docNumber\":\"" + documento + "\"}"))
                .andExpect(status().isCreated());

        String clienteId = idDelUltimoCliente(taller);

        mockMvc.perform(delete("/api/v1/fleet/customers/{id}", clienteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/fleet/customers/{id}", clienteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVO"));
    }

    @Test
    @DisplayName("Un cliente con vehiculos no se puede dar de baja")
    void unClienteConVehiculosNoSePuedeDarDeBaja() throws Exception {
        UUID taller = UUID.randomUUID();
        String token = TestJwksServer.signToken(taller, List.of("ADMIN_TALLER"));
        String documento = documento();

        mockMvc.perform(post("/api/v1/fleet/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Cliente con vehiculo\",\"docType\":\"RUC\",\"docNumber\":\"" + documento + "\"}"))
                .andExpect(status().isCreated());

        String clienteId = idDelUltimoCliente(taller);

        mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + clienteId + "\",\"plate\":\"XYZ-%03d\"}".formatted(Math.abs(UUID.randomUUID().hashCode() % 1000))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/fleet/customers/{id}", clienteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FLEET_CUSTOMER_HAS_VEHICLES"));

        mockMvc.perform(get("/api/v1/fleet/customers/{id}", clienteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVO"));
    }

    @Test
    @DisplayName("El listado de vehiculos de un cliente esta acotado al taller")
    void elListadoDeVehiculosDeUnClienteEstaAcotadoAlTaller() throws Exception {
        UUID taller = UUID.randomUUID();
        UUID otroTaller = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/fleet/customers")
                        .header("Authorization", "Bearer " + TestJwksServer.signToken(taller, List.of("ADMIN_TALLER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Cliente del taller\",\"docType\":\"RUC\",\"docNumber\":\"" + documento() + "\"}"))
                .andExpect(status().isCreated());

        String clienteId = idDelUltimoCliente(taller);

        mockMvc.perform(get("/api/v1/fleet/vehicles/customer/{customerId}", clienteId)
                        .header("Authorization", "Bearer " + TestJwksServer.signToken(otroTaller, List.of("ADMIN_TALLER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private String idDelUltimoCliente(UUID tenantId) {
        TenantContextHolder.setTenantId(tenantId);
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
            TenantContextHolder.clear();
        }
    }

    private String documento() {
        return String.valueOf(Math.abs(UUID.randomUUID().hashCode() % 1000000000)) + "13";
    }
}
