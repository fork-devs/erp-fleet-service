package com.erp.fleet.infrastructure.client;

import com.erp.fleet.domain.exception.ValidacionNoDisponibleException;
import com.erp.fleet.domain.model.ResultadoValidacionIdentidad;
import com.erp.fleet.domain.model.ResultadoValidacionVehiculo;
import com.erp.fleet.domain.port.ValidacionIdentidadPort;
import com.erp.fleet.domain.port.ValidacionVehiculoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/**
 * Cliente del hub para la validacion de identidad y de vehiculo.
 *
 * <p>El hub es el UNICO punto de salida a RENIEC, SUNAT y la consulta
 * vehicular: fleet nunca los llama directamente.
 *
 * <p>El hub responde 429 cuando el taller ha agotado su cuota de
 * validaciones, y el limite es por taller: por eso la peticion lleva el
 * taller del contexto.
 *
 * <p>Un fallo del hub NO se traduce a un resultado: se lanza la excepcion,
 * para que el caso de uso deje el registro pendiente y programe el
 * reintento, en lugar de marcarlo como no valido.
 */
public class HubValidacionAdapter implements ValidacionIdentidadPort, ValidacionVehiculoPort {

    private static final Logger log = LoggerFactory.getLogger(HubValidacionAdapter.class);

    private final RestClient restClient;
    private final String serviceKey;
    private final String serviceName;

    public HubValidacionAdapter(RestClient restClient, String serviceKey, String serviceName) {
        this.restClient = restClient;
        this.serviceKey = serviceKey;
        this.serviceName = serviceName;
    }

    @Override
    public ResultadoValidacionIdentidad validarDocumento(String ruc) {
        UUID tenantId = tenantDelContexto();
        try {
            RucResponse respuesta = restClient.get()
                    .uri("/api/v1/integrations/validate/ruc/{ruc}", ruc)
                    .header("X-Internal-Service-Key", serviceKey)
                    .header("X-Internal-Service-Name", serviceName)
                    .header("X-Tenant-ID", tenantId.toString())
                    .retrieve()
                    .body(RucResponse.class);

            if (respuesta == null) {
                throw new ValidacionNoDisponibleException(
                        "El hub no ha devuelto respuesta al validar el RUC " + ruc);
            }

            if (!respuesta.esValido()) {
                return ResultadoValidacionIdentidad.observado();
            }

            return ResultadoValidacionIdentidad.valido(respuesta.razonSocial(), respuesta.estado(),
                    respuesta.condicion(), respuesta.direccionFiscal());
        } catch (ValidacionNoDisponibleException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("El hub no ha podido validar el RUC {}: {}", ruc, e.getMessage());
            throw new ValidacionNoDisponibleException(
                    "No se ha podido validar el RUC " + ruc + ": el hub no esta disponible", e);
        }
    }

    @Override
    public ResultadoValidacionVehiculo validarPlaca(String placa) {
        UUID tenantId = tenantDelContexto();
        try {
            PlacaResponse respuesta = restClient.get()
                    .uri("/api/v1/integrations/validate/placa/{placa}", placa)
                    .header("X-Internal-Service-Key", serviceKey)
                    .header("X-Internal-Service-Name", serviceName)
                    .header("X-Tenant-ID", tenantId.toString())
                    .retrieve()
                    .body(PlacaResponse.class);

            if (respuesta == null) {
                throw new ValidacionNoDisponibleException(
                        "El hub no ha devuelto respuesta al validar la placa " + placa);
            }

            if (!respuesta.esValido()) {
                return ResultadoValidacionVehiculo.observado();
            }

            return ResultadoValidacionVehiculo.valido(respuesta.vin(), respuesta.numeroMotor(),
                    respuesta.marca(), respuesta.modelo(), respuesta.color(), respuesta.combustible());
        } catch (ValidacionNoDisponibleException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("El hub no ha podido validar la placa {}: {}", placa, e.getMessage());
            throw new ValidacionNoDisponibleException(
                    "No se ha podido validar la placa " + placa + ": el hub no esta disponible", e);
        }
    }

    private UUID tenantDelContexto() {
        UUID tenantId = com.erp.fleet.infrastructure.persistence.rls.TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new com.erp.fleet.domain.exception.TenantContextMissingException(
                    "La validacion requiere un contexto de taller establecido y no lo tiene.");
        }
        return tenantId;
    }
}
