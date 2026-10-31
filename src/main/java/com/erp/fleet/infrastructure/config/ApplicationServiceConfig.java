package com.erp.fleet.infrastructure.config;

import com.erp.fleet.application.service.AppointmentApplicationService;
import com.erp.fleet.application.service.CustomerManagementService;
import com.erp.fleet.application.service.RevalidationService;
import com.erp.fleet.application.service.ValidationService;
import com.erp.fleet.application.service.VehicleManagementService;
import com.erp.fleet.application.usecase.ValidateCustomerUseCase;
import com.erp.fleet.application.usecase.ValidateVehicleUseCase;
import com.erp.fleet.domain.port.AppointmentRepositoryPort;
import com.erp.fleet.domain.port.CustomerRepositoryPort;
import com.erp.fleet.domain.port.IdentityValidationPort;
import com.erp.fleet.domain.port.PendingValidationsPort;
import com.erp.fleet.domain.port.TenantContextPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.port.ValidationCachePort;
import com.erp.fleet.domain.port.VehicleRepositoryPort;
import com.erp.fleet.domain.port.VehicleValidationPort;
import com.erp.fleet.infrastructure.client.HubValidationAdapter;
import com.erp.fleet.infrastructure.security.JwksKeyManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Arrays;

/**
 * Spring configuration wiring application services, use cases, and client adapters.
 */
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
    public AppointmentApplicationService appointmentApplicationService(AppointmentRepositoryPort appointmentRepository,
                                                                       CustomerRepositoryPort customerRepository,
                                                                       VehicleRepositoryPort vehicleRepository,
                                                                       UnitOfWork unitOfWork) {
        return new AppointmentApplicationService(appointmentRepository, customerRepository, vehicleRepository, unitOfWork);
    }

    @Bean
    public ValidationService validationService(CustomerRepositoryPort customerRepository,
                                               VehicleRepositoryPort vehicleRepository,
                                               IdentityValidationPort identityValidation,
                                               VehicleValidationPort vehicleValidation,
                                               ValidationCachePort cache,
                                               UnitOfWork unitOfWork) {
        return new ValidationService(customerRepository, vehicleRepository, identityValidation,
                vehicleValidation, cache, unitOfWork);
    }

    @Bean
    public RevalidationService revalidationService(PendingValidationsPort pendingValidations,
                                                   TenantContextPort tenantContext,
                                                   ValidateCustomerUseCase validateCustomer,
                                                   ValidateVehicleUseCase validateVehicle) {
        return new RevalidationService(pendingValidations, tenantContext, validateCustomer, validateVehicle);
    }

    @Bean
    public JwksKeyManager jwksKeyManager(
            @Value("${erp.auth.jwks-url}") String jwksUrl,
            Environment environment) {
        return new JwksKeyManager(jwksUrl, Arrays.asList(environment.getActiveProfiles()));
    }

    @Bean
    public RestClient hubRestClient(
            @Value("${erp.integration-hub.url}") String hubUrl) {
        return RestClient.builder()
                .baseUrl(hubUrl)
                .requestFactory(requestFactory(Duration.ofSeconds(3)))
                .build();
    }

    @Bean
    public HubValidationAdapter hubValidationAdapter(RestClient hubRestClient,
                                                     @Value("${erp.integration-hub.key:}") String serviceKey,
                                                     @Value("${erp.integration-hub.service-name:erp-fleet-service}")
                                                     String serviceName) {
        return new HubValidationAdapter(hubRestClient, serviceKey, serviceName);
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
