package com.erp.fleet.infrastructure.messaging;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import javax.sql.DataSource;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "erp.appointment-email", name = "enabled", havingValue = "true")
public class AppointmentEmailRelay {
    private static final DateTimeFormatter LIMA_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm")
            .withZone(ZoneId.of("America/Lima"));

    private final JdbcClient jdbc;
    private final TransactionTemplate transaction;
    private final RestClient mail;
    private final String endpoint;
    private final String sender;

    public AppointmentEmailRelay(@Qualifier("platformDataSource") DataSource dataSource,
                                 @Value("${erp.appointment-email.endpoint}") String endpoint,
                                 @Value("${erp.appointment-email.key}") String key,
                                 @Value("${erp.appointment-email.sender}") String sender) {
        if (key.isBlank() || sender.isBlank()) {
            throw new IllegalArgumentException("Configura la clave y el remitente del correo de citas");
        }
        jdbc = JdbcClient.create(dataSource);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(4));
        mail = RestClient.builder().requestFactory(factory).defaultHeader("api-key", key).build();
        this.endpoint = endpoint;
        this.sender = sender;
    }

    @Scheduled(fixedDelayString = "${erp.appointment-email.interval}")
    public void dispatch() {
        // The same transaction guards the queue row until the provider replies.
        transaction.executeWithoutResult(status -> jdbc.sql("""
                SELECT e.appointment_id, e.recipient, e.scheduled_start,
                       a.status IN ('SCHEDULED', 'CONFIRMED') AND a.scheduled_start = e.scheduled_start AS active
                FROM appointment_email_outbox e JOIN appointments a ON a.id = e.appointment_id
                WHERE e.sent_at IS NULL AND e.stopped_at IS NULL AND e.next_attempt <= now()
                ORDER BY e.next_attempt, e.appointment_id LIMIT 1 FOR UPDATE OF e SKIP LOCKED
                """).query((rs, n) -> new QueuedEmail(rs.getObject("appointment_id", UUID.class),
                rs.getString("recipient"), rs.getObject("scheduled_start", OffsetDateTime.class),
                rs.getBoolean("active"))).optional().ifPresent(this::send));
    }

    private void send(QueuedEmail email) {
        if (!email.active()) {
            jdbc.sql("UPDATE appointment_email_outbox SET stopped_at = now() WHERE appointment_id = ?")
                    .param(email.id()).update();
        } else if (deliver(email)) {
            jdbc.sql("UPDATE appointment_email_outbox SET sent_at = now() WHERE appointment_id = ?")
                    .param(email.id()).update();
        } else {
            jdbc.sql("""
                    UPDATE appointment_email_outbox
                    SET attempts = attempts + 1, next_attempt = now() + interval '2 minutes',
                        stopped_at = CASE WHEN attempts >= 4 THEN now() END
                    WHERE appointment_id = ?
                    """).param(email.id()).update();
        }
    }

    private boolean deliver(QueuedEmail email) {
        try {
            mail.post().uri(endpoint).contentType(MediaType.APPLICATION_JSON).body(Map.of(
                    "sender", Map.of("email", sender),
                    "to", List.of(Map.of("email", email.recipient())),
                    "headers", Map.of("idempotencyKey", email.id().toString()),
                    "subject", "DriveOS | Cita de mantenimiento programada",
                    "textContent", "La cita fue programada para el " + LIMA_DATE.format(email.start())
                            + " (hora de Lima). Referencia: " + email.id()
            )).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientResponseException e) {
            return e.getStatusCode().value() == 400 && e.getResponseBodyAsString().contains("\"duplicate_parameter\"")
                    && e.getResponseBodyAsString().toLowerCase(java.util.Locale.ROOT).contains("idempotency");
        } catch (RestClientException e) {
            return false;
        }
    }

    private record QueuedEmail(UUID id, String recipient, OffsetDateTime start, boolean active) {}
}
