package com.erp.fleet.infrastructure.scheduler;

import com.erp.fleet.application.service.RevalidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task that periodically runs the revalidation sweep for pending entities.
 */
@Component
public class PendingValidationScheduler {

    private static final Logger log = LoggerFactory.getLogger(PendingValidationScheduler.class);

    private final RevalidationService revalidationService;
    private final int maxPerTenant;

    public PendingValidationScheduler(RevalidationService revalidationService,
                                      @Value("${erp.revalidacion.max-por-taller:10}") int maxPerTenant) {
        this.revalidationService = revalidationService;
        this.maxPerTenant = maxPerTenant;
    }

    @Scheduled(fixedDelayString = "${erp.revalidacion.intervalo:60000}")
    public void execute() {
        try {
            revalidationService.execute(maxPerTenant);
        } catch (Exception e) {
            log.error("Scheduled revalidation sweep failed: {}", e.getMessage(), e);
        }
    }

    // Compatibility alias
    public void ejecutar() {
        execute();
    }
}
