package com.erp.fleet.domain.model;

public enum AppointmentStatus {
    SCHEDULED,
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public boolean isActive() {
        return this == SCHEDULED || this == CONFIRMED || this == IN_PROGRESS;
    }
}
