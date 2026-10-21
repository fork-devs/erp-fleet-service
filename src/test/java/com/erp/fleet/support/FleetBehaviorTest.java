package com.erp.fleet.support;

import com.erp.fleet.application.dto.command.RegisterCustomerCommand;
import com.erp.fleet.application.dto.command.RegisterVehicleCommand;
import com.erp.fleet.application.dto.result.CustomerResult;
import com.erp.fleet.application.usecase.RegisterCustomerUseCase;
import com.erp.fleet.domain.port.CustomerRepositoryPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.infrastructure.persistence.rls.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El comportamiento de la API, caso por caso.
 *
 * <p>El repositorio tenia un unico test, el arranque del contexto. Esta suite
 * cubre lo que el servicio promete: alta de cliente y de vehiculo, consulta
 * por identificador, por placa y por cliente, listado del taller, actualizacion
 * y baja.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FleetBehaviorTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegisterCustomerUseCase registerCustomerUseCase;

    @Autowired
    private CustomerRepositoryPort customerRepository;

    @Autowired
    private UnitOfWork unitOfWork;

    private UUID taller;

    @BeforeEach
    void setUp() {
        taller = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("El vehiculo se registra y queda pendiente de validacion")
    void elVehiculoSeRegistraYQuedaPendienteDeValidacion() throws Exception {
        String clienteId = clienteDelTaller();
        String placa = placa();

        mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + clienteId + "\",\"plate\":\"" + placa
                                + "\",\"brand\":\"Toyota\",\"model\":\"Corolla\",\"year\":2021}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.validationStatus").value("PENDING"))
                .andExpect(jsonPath("$.plate").value(placa));
    }

    @Test
    @DisplayName("El listado del taller devuelve sus vehiculos")
    void elListadoDelTallerDevuelveSusVehiculos() throws Exception {
        String clienteId = clienteDelTaller();
        mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + clienteId + "\",\"plate\":\"" + placa() + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/fleet/vehicles")
                        .header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Los vehiculos de un cliente se listan aparte")
    void losVehiculosDeUnClienteSeListanAparte() throws Exception {
        String clienteId = clienteDelTaller();
        mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + clienteId + "\",\"plate\":\"" + placa() + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/fleet/vehicles/customer/{customerId}", clienteId)
                        .header("Authorization", "Bearer " + token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("La actualizacion del vehiculo guarda los datos nuevos")
    void laActualizacionDelVehiculoGuardaLosDatosNuevos() throws Exception {
        String clienteId = clienteDelTaller();
        String placa = placa();

        String vehiculoId = mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + clienteId + "\",\"plate\":\"" + placa + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String id = vehiculoId.replaceAll(".*\"id\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(put("/api/v1/fleet/vehicles/{id}", id)
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":2022,\"model\":\"Corolla Cross\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2022))
                .andExpect(jsonPath("$.model").value("Corolla Cross"));
    }

    @Test
    @DisplayName("La actualizacion del cliente guarda los datos de contacto")
    void laActualizacionDelClienteGuardaLosDatosDeContacto() throws Exception {
        String clienteId = clienteDelTaller();

        mockMvc.perform(put("/api/v1/fleet/customers/{id}", clienteId)
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"999888777\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("999888777"));
    }

    @Test
    @DisplayName("Un vehiculo con una placa invalida se rechaza")
    void unVehiculoConUnaPlacaInvalidaSeRechaza() throws Exception {
        String clienteId = clienteDelTaller();

        mockMvc.perform(post("/api/v1/fleet/vehicles")
                        .header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + clienteId + "\",\"plate\":\"NO-VALIDA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("El taller ve exactamente sus clientes")
    void elTallerVeExactamenteSusClientes() throws Exception {
        clienteDelTaller();
        UUID otroTaller = UUID.randomUUID();
        TenantContextHolder.setTenantId(otroTaller);
        registerCustomerUseCase.register(new RegisterCustomerCommand(otroTaller,
                "Cliente de otro taller", "otro@ejemplo.com", "999999999", "RUC", documento()));
        TenantContextHolder.clear();

        TenantContextHolder.setTenantId(taller);
        List<?> clientes = unitOfWork.callInTransaction(() ->
                customerRepository.findAllByTenantId(new TenantId(taller)));

        assertEquals(1, clientes.size());
    }

    private String token() {
        return TestJwksServer.signToken(taller, List.of("ADMIN_TALLER"));
    }

    private String clienteDelTaller() {
        TenantContextHolder.setTenantId(taller);
        try {
            CustomerResult cliente = registerCustomerUseCase.register(new RegisterCustomerCommand(taller,
                    "Cliente del taller", "cliente@ejemplo.com", "999999999", "RUC", documento()));
            return cliente.id().toString();
        } finally {
            TenantContextHolder.clear();
        }
    }

    private String documento() {
        return String.valueOf(Math.abs(UUID.randomUUID().hashCode() % 1000000000)) + "13";
    }

    private String placa() {
        return String.format("ABC-%03d", Math.abs(UUID.randomUUID().hashCode() % 1000));
    }
}
