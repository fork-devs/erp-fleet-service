package com.erp.fleet.infrastructure.scheduler;

import com.erp.fleet.application.service.RevalidacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Barrido programado de revalidacion.
 *
 * <p>Es lo que hace que la validacion se reintente sola: sin el, un registro
 * que se quedo pendiente por una caida del hub necesita que alguien lo pida a
 * mano, y el fallo del proveedor se convierte en trabajo manual.
 */
@Component
public class ValidacionPendienteScheduler {

    private static final Logger log = LoggerFactory.getLogger(ValidacionPendienteScheduler.class);

    private final RevalidacionService revalidacionService;
    private final int maxPorTaller;

    public ValidacionPendienteScheduler(RevalidacionService revalidacionService,
                                         @Value("${erp.revalidacion.max-por-taller:10}") int maxPorTaller) {
        this.revalidacionService = revalidacionService;
        this.maxPorTaller = maxPorTaller;
    }

    @Scheduled(fixedDelayString = "${erp.revalidacion.intervalo:60000}")
    public void ejecutar() {
        try {
            revalidacionService.ejecutar(maxPorTaller);
        } catch (Exception e) {
            // Un fallo del barrido no debe tumbar el servicio: el siguiente
            // intento lo recogera.
            log.error("El barrido de revalidacion ha fallado: {}", e.getMessage(), e);
        }
    }
}
