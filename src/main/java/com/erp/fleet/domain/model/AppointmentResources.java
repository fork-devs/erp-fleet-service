package com.erp.fleet.domain.model;

import java.util.UUID;

public record AppointmentResources(Integer bayNumber, UUID technicianId) {
    public AppointmentResources {
        if (bayNumber == null || bayNumber < 1 || technicianId == null) {
            throw new IllegalArgumentException("Selecciona una bahía y un técnico válidos");
        }
    }

    public static AppointmentResources from(Integer bayNumber, UUID technicianId) {
        if (bayNumber == null && technicianId == null) {
            return null;
        }
        return new AppointmentResources(bayNumber, technicianId);
    }
}
