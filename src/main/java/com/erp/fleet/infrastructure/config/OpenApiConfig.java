package com.erp.fleet.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Documento OpenAPI de la API.
 *
 * <p>El documento declara el rol que exige cada endpoint, leido de la propia
 * anotacion que lo exige: de este modo la documentacion no puede separarse
 * del codigo, porque sale de el.
 */
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

    /**
     * Añade a cada operacion el rol que exige, deducido de la anotacion
     * {@code @PreAuthorize} del metodo.
     */
    @Bean
    public OperationCustomizer rolesPorEndpoint() {
        return (operation, handlerMethod) -> {
            PreAuthorize anotacion = AnnotatedElementUtils
                    .findMergedAnnotation(handlerMethod.getMethod(), PreAuthorize.class);
            if (anotacion == null) {
                return operation;
            }
            String requisito = "Requiere: " + anotacion.value();
            operation.setDescription(operation.getDescription() == null
                    ? requisito
                    : operation.getDescription() + " | " + requisito);
            return operation;
        };
    }
}
