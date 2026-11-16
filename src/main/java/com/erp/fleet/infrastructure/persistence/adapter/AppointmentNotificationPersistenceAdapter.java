package com.erp.fleet.infrastructure.persistence.adapter;

import com.erp.fleet.domain.model.Appointment;
import com.erp.fleet.domain.port.AppointmentNotificationPort;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Repository
public class AppointmentNotificationPersistenceAdapter implements AppointmentNotificationPort {
    private final JdbcClient jdbc;
    private final EntityManager entityManager;

    public AppointmentNotificationPersistenceAdapter(DataSource dataSource, EntityManager entityManager) {
        this.entityManager = entityManager;
        jdbc = JdbcClient.create(dataSource);
    }

    @Override
    public void enqueue(Appointment appointment, String recipient) {
        entityManager.flush();
        jdbc.sql("""
                INSERT INTO appointment_email_outbox (appointment_id, tenant_id, recipient, scheduled_start)
                VALUES (?, ?, ?, ?)
                """).params(appointment.getId().getValue(), appointment.getTenantId().getValue(), recipient,
                OffsetDateTime.ofInstant(appointment.getScheduledStart(), ZoneOffset.UTC)).update();
    }
}
