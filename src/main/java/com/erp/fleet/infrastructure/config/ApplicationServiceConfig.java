package com.erp.fleet.infrastructure.config;

import com.erp.fleet.application.service.CustomerManagementService;
import com.erp.fleet.application.service.VehicleManagementService;
import com.erp.fleet.domain.port.CustomerRepositoryPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.port.VehicleRepositoryPort;
import com.erp.fleet.infrastructure.security.JwksKeyManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
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
}
