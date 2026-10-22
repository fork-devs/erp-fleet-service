package com.erp.fleet.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * La salud y la disponibilidad se comprueban de verdad.
 *
 * <p>El servicio no tiene forma de saber si responde, ni si su base de datos
 * es alcanzable, que es exactamente lo que un orquestador necesita para
 * decidir si le manda trafico.
 *
 * <p>En el entorno de test no hay cache: la comprobacion falla de verdad, y el
 * servicio informa de NO disponible. Es el comportamiento que se quiere: una
 * comprobacion que no puede ejecutarse no puede informar de que todo esta bien.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FleetHealthTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("La salud comprueba la base de datos de verdad")
    void laSaludCompruebaLaBaseDeDatosDeVerdad() throws Exception {
        String respuesta = mockMvc.perform(get("/actuator/health"))
                .andReturn().getResponse().getContentAsString();

        assertTrue(respuesta.contains("fleetDatabase"),
                "La salud debe incluir la comprobacion real de la base de datos");
        assertTrue(respuesta.contains("\"fleetDatabase\":{\"details\":{\"comprobacion\":\"consulta ejecutada\"},\"status\":\"UP\"}"),
                "La comprobacion de la base de datos se ejecuta de verdad: " + respuesta);
    }

    @Test
    @DisplayName("La disponibilidad informa del fallo de una comprobacion real")
    void laDisponibilidadInformaDelFalloDeUnaComprobacionReal() throws Exception {
        var resultado = mockMvc.perform(get("/actuator/health/readiness"))
                .andReturn().getResponse();

        String respuesta = resultado.getContentAsString();

        assertTrue(respuesta.contains("fleetReadiness"),
                "La disponibilidad debe usar la comprobacion propia del servicio");
        assertTrue(respuesta.contains("\"status\":\"DOWN\""),
                "Con una comprobacion real fallida, el servicio no informa de disponibilidad: " + respuesta);
        assertTrue(resultado.getStatus() == 503,
                "El servicio responde 503 cuando no esta disponible, para que el orquestador deje "
                        + "de mandarle trafico");
    }
}
