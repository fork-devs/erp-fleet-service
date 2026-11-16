package com.erp.fleet.domain.port;

import com.erp.fleet.domain.model.Appointment;

public interface AppointmentNotificationPort {
    void enqueue(Appointment appointment, String recipient);
}
