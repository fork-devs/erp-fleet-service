package com.erp.fleet.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El documento OpenAPI declara los endpoints y el rol que exige cada uno.
 *
 * <p>El rol sale de la propia anotacion que lo exige, de modo que la
 * documentacion no puede separarse del codigo.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FleetOpenApiTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("El documento declara los endpoints con el rol que exigen")
    void elDocumentoDeclaraLosEndpointsConElRolQueExigen() throws Exception {
        String documento = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertTrue(documento.contains("/api/v1/fleet/customers"),
                "El documento debe declarar los endpoints bajo el prefijo que reenvia el gateway");
        assertTrue(documento.contains("/api/v1/fleet/vehicles"));
        assertTrue(documento.contains("/api/v1/fleet/customers/{id}/validate"),
                "El documento debe declarar el reintento de la validacion");
        assertTrue(documento.contains("ADMIN_TALLER"),
                "El documento debe declarar el rol que exige cada endpoint");
    }
}
