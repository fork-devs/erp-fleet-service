package com.erp.fleet.support;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La API exige un token verificado, y el taller lo decide el token.
 *
 * <p>El servicio publicaba el comodin {@code /api/v1/**} en permitAll y leia
 * el taller de la cabecera {@code X-Tenant-ID}. Con eso, cualquiera que
 * alcanzara el servicio podia crear, listar, actualizar y dar de baja clientes
 * y vehiculos de cualquier taller, sin token.
 *
 * <p>Ver docs/doc-estado-implementacion.md, hallazgos P0 5.1, P0 5.2, P0 5.4,
 * P1 6.6, P1 6.14, P1 6.19 y P1 6.53.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FleetSecurityTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Una peticion sin token se rechaza y no escribe nada")
    void unaPeticionSinTokenSeRechaza() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Cliente sin token\",\"docType\":\"DNI\",\"docNumber\":\"99999999\"}"))
                .andExpect(status().isUnauthorized());

        assertEquals(0, contarClientesConDocumento("99999999"),
                "Sin token no se debe escribir ningun cliente");
    }

    /**
     * Cuenta los clientes con un documento concreto directamente en la base de
     * datos: la operacion de negocio exige un contexto de taller, y aqui lo
     * que se comprueba es que no se escribio nada.
     */
    private int contarClientesConDocumento(String docNumber) {
        try (java.sql.Connection connection = EmbeddedPostgresSupport.openOwnerConnection();
             java.sql.Statement statement = connection.createStatement();
             java.sql.ResultSet rs = statement.executeQuery(
                     "SELECT count(*) FROM customers WHERE doc_number = '" + docNumber + "'")) {
            rs.next();
            return rs.getInt(1);
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private int contarClientesDelTaller(UUID tenantId) {
        try (java.sql.Connection connection = EmbeddedPostgresSupport.openOwnerConnection();
             java.sql.Statement statement = connection.createStatement();
             java.sql.ResultSet rs = statement.executeQuery(
                     "SELECT count(*) FROM customers WHERE tenant_id = '" + tenantId + "'")) {
            rs.next();
            return rs.getInt(1);
        } catch (java.sql.SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("Un token con firma invalida se rechaza")
    void unTokenConFirmaInvalidaSeRechaza() throws Exception {
        String token = TestJwksServer.signToken(UUID.randomUUID(), List.of("ADMIN_TALLER"));
        String adulterado = token.substring(0, token.length() - 4) + "abcd";

        mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un rol sin permiso de escritura recibe 403")
    void unRolSinPermisoDeEscrituraRecibe403() throws Exception {
        UUID taller = UUID.randomUUID();
        String token = TestJwksServer.signToken(taller, List.of("MECANICO"));

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Cliente de mecanico\",\"docType\":\"DNI\",\"docNumber\":\"88888888\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un rol de solo lectura puede consultar")
    void unRolDeSoloLecturaPuedeConsultar() throws Exception {
        UUID taller = UUID.randomUUID();
        String token = TestJwksServer.signToken(taller, List.of("MECANICO"));

        mockMvc.perform(get("/api/v1/customers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un rol con permiso de escritura puede registrar")
    void unRolConPermisoDeEscrituraPuedeRegistrar() throws Exception {
        UUID taller = UUID.randomUUID();
        String token = TestJwksServer.signToken(taller, List.of("ADMIN_TALLER"));

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Cliente de administrador\",\"docType\":\"DNI\",\"docNumber\":\"77777777\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("El taller del cuerpo de la peticion se ignora")
    void elTallerDelCuerpoDeLaPeticionSeIgnora() throws Exception {
        UUID taller = UUID.randomUUID();
        UUID otroTaller = UUID.randomUUID();
        String token = TestJwksServer.signToken(taller, List.of("ADMIN_TALLER"));
        String documento = "666666" + Math.abs(UUID.randomUUID().hashCode() % 1000);

        mockMvc.perform(post("/api/v1/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Cliente con taller en el cuerpo\",\"docType\":\"DNI\","
                                + "\"docNumber\":\"" + documento + "\",\"tenantId\":\"" + otroTaller + "\"}"))
                .andExpect(status().isCreated());

        assertEquals(1, contarClientesConDocumento(documento),
                "El cliente se registra en el taller del token");
        assertEquals(0, contarClientesDelTaller(otroTaller),
                "No se debe escribir nada en el taller que declara el cuerpo");
    }

    @Test
    @DisplayName("Una cabecera de taller que no coincide con el token se rechaza")
    void unaCabeceraDeTallerDistintaDelTokenSeRechaza() throws Exception {
        UUID tallerDelToken = UUID.randomUUID();
        UUID otroTaller = UUID.randomUUID();
        String token = TestJwksServer.signToken(tallerDelToken, List.of("ADMIN_TALLER"));

        mockMvc.perform(get("/api/v1/customers")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-ID", otroTaller.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Una cabecera de taller que coincide con el token se acepta")
    void unaCabeceraDeTallerCoincidenteSeAcepta() throws Exception {
        UUID taller = UUID.randomUUID();
        String token = TestJwksServer.signToken(taller, List.of("ADMIN_TALLER"));

        mockMvc.perform(get("/api/v1/customers")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-ID", taller.toString()))
                .andExpect(status().isOk());
    }
}
