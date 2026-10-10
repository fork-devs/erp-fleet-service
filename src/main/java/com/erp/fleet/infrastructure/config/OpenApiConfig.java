package com.erp.fleet.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI fleetOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ERP Fleet Service API")
                        .description("Microservicio de Gestión de Clientes y Vehículos — DriveOS ERP")
                        .version("v1.0.0"));
    }
}
