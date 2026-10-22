package com.erp.fleet.infrastructure.config;

import com.erp.fleet.application.service.CustomerManagementService;
import com.erp.fleet.application.service.RevalidacionService;
import com.erp.fleet.application.service.ValidacionService;
import com.erp.fleet.application.service.VehicleManagementService;
import com.erp.fleet.domain.port.CustomerRepositoryPort;
import com.erp.fleet.domain.port.PendientesValidacionPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.port.VehicleRepositoryPort;
import com.erp.fleet.infrastructure.cache.ValidacionCacheAdapter;
import com.erp.fleet.infrastructure.client.HubValidacionAdapter;
import com.erp.fleet.infrastructure.security.JwksKeyManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Arrays;

@Configuration
@EnableScheduling
public class ApplicationServiceConfig {

    @Bean
    public CustomerManagementService customerManagementService(CustomerRepositoryPort port,
                                                                VehicleRepositoryPort vehiclePort,
                                                                UnitOfWork unitOfWork) {
        return new CustomerManagementService(port, vehiclePort, unitOfWork);
    }

    @Bean
    public VehicleManagementService vehicleManagementService(VehicleRepositoryPort port,
                                                            CustomerRepositoryPort customerPort,
                                                            UnitOfWork unitOfWork) {
        return new VehicleManagementService(port, customerPort, unitOfWork);
    }

    @Bean
    public ValidacionService validacionService(CustomerRepositoryPort customerRepository,
                                               VehicleRepositoryPort vehicleRepository,
                                               com.erp.fleet.domain.port.ValidacionIdentidadPort validacionIdentidad,
                                               com.erp.fleet.domain.port.ValidacionVehiculoPort validacionVehiculo,
                                               com.erp.fleet.domain.port.ValidacionCachePort cache,
                                               UnitOfWork unitOfWork) {
        return new ValidacionService(customerRepository, vehicleRepository, validacionIdentidad,
                validacionVehiculo, cache, unitOfWork);
    }

    @Bean
    public RevalidacionService revalidacionService(PendientesValidacionPort pendientes,
                                                   com.erp.fleet.domain.port.TenantContextPort tenantContext,
                                                   com.erp.fleet.application.usecase.ValidarClienteUseCase validarCliente,
                                                   com.erp.fleet.application.usecase.ValidarVehiculoUseCase validarVehiculo) {
        return new RevalidacionService(pendientes, tenantContext, validarCliente, validarVehiculo);
    }

    /**
     * Cliente del conjunto de claves del emisor de credenciales.
     *
     * <p>La URL debe usar un esquema cifrado fuera del perfil de desarrollo
     * local: descargar las claves en claro permite que cualquiera en el camino
     * las sustituya y firme tokens que el servicio aceptaria.
     */
    @Bean
    public JwksKeyManager jwksKeyManager(
            @Value("${erp.auth.jwks-url}") String jwksUrl,
            Environment environment) {
        return new JwksKeyManager(jwksUrl, Arrays.asList(environment.getActiveProfiles()));
    }

    /**
     * Cliente HTTP del hub de integraciones.
     *
     * <p>El tiempo de espera es corto a proposito: la validacion no esta en el
     * camino critico del taller —el registro ya esta confirmado—, y una
     * llamada colgada no puede dejar al taller esperando.
     */
    @Bean
    public RestClient hubRestClient(
            @Value("${erp.integration-hub.url}") String hubUrl) {
        return RestClient.builder()
                .baseUrl(hubUrl)
                .requestFactory(requestFactory(Duration.ofSeconds(3)))
                .build();
    }

    /**
     * Adapter del hub para la validacion de identidad y de vehiculo.
     *
     * <p>Un unico adapter implementa los dos puertos: el hub expone las dos
     * validaciones con el mismo contrato de servicio.
     */
    @Bean
    public HubValidacionAdapter hubValidacionAdapter(RestClient hubRestClient,
                                                     @Value("${erp.integration-hub.key:}") String serviceKey,
                                                     @Value("${erp.integration-hub.service-name:erp-fleet-service}")
                                                     String serviceName) {
        return new HubValidacionAdapter(hubRestClient, serviceKey, serviceName);
    }

    private org.springframework.http.client.ClientHttpRequestFactory requestFactory(Duration timeout) {
        java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(timeout);
        return factory;
    }
}
