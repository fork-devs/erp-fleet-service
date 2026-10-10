package com.erp.fleet.infrastructure.config;

import com.erp.fleet.application.service.CustomerManagementService;
import com.erp.fleet.application.service.VehicleManagementService;
import com.erp.fleet.domain.port.CustomerRepositoryPort;
import com.erp.fleet.domain.port.VehicleRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationServiceConfig {
    @Bean
    public CustomerManagementService customerManagementService(CustomerRepositoryPort port) {
        return new CustomerManagementService(port);
    }

    @Bean
    public VehicleManagementService vehicleManagementService(VehicleRepositoryPort port) {
        return new VehicleManagementService(port);
    }
}
