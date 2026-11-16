# erp-fleet-service

Microservicio de **Customer & Fleet** para el ERP Automotriz SaaS DriveOS.

## Responsabilidad
Gestión de clientes y vehículos (placa/VIN/RUC) por taller (tenant). Validación asíncrona via `integration-hub` con caché Redis. Si RENIEC cae, el registro queda como `PENDING` y se reintenta.

## Stack
- Java 21 / Spring Boot 4.1.1
- PostgreSQL 16 + Flyway + RLS por tenant_id
- Puerto: 8083

## Quickstart
```bash
docker compose up -d
mvn spring-boot:run
```

## Endpoints principales
| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | /api/v1/customers | Registrar cliente |
| GET | /api/v1/customers | Listar clientes |
| GET | /api/v1/customers/{id} | Obtener cliente |
| PUT | /api/v1/customers/{id} | Actualizar cliente |
| DELETE | /api/v1/customers/{id} | Desactivar cliente |
| POST | /api/v1/vehicles | Registrar vehículo |
| GET | /api/v1/vehicles | Listar vehículos |
| GET | /api/v1/vehicles/{id} | Obtener vehículo |
| GET | /api/v1/vehicles/customer/{id} | Vehículos de un cliente |
| PUT | /api/v1/vehicles/{id} | Actualizar vehículo |

Swagger UI: http://localhost:8083/swagger-ui.html
### Citas preventivas con bahía y técnico

Flyway agrega el catálogo de bahías de cada sede y las asignaciones de recursos en la migración V7.

- Un administrador da de alta o renombra una bahía mediante `PUT /api/v1/fleet/appointments/resources/bays/{number}?branchId={branchId}` con `{"name":"Elevador"}`. Flota valida la sede contra Tenant Service.
- `GET /api/v1/fleet/appointments/resources?branchId={branchId}` devuelve el catálogo y los técnicos con inscripción activa en la sede. Para cargar el catálogo desde otra ubicación, configura `TENANT_SERVICE_URL`.
- La programación desde alertas valida la disponibilidad por vehículo, bahía y técnico y devuelve la asignación en la cita. Las citas anteriores conservan su formato sin asignación.

Flota guarda la confirmación por correo en `appointment_email_outbox` en la misma transacción que la cita, si el cliente tiene correo. Activa el envío con `APPOINTMENT_EMAIL_ENABLED=true`, `APPOINTMENT_EMAIL_API_KEY` y `APPOINTMENT_EMAIL_SENDER`. El remitente debe estar validado por Brevo. El envío queda desactivado por defecto. `APPOINTMENT_EMAIL_ENDPOINT` permite apuntar a un servidor de pruebas y `APPOINTMENT_EMAIL_INTERVAL` controla la frecuencia de revisión de la cola. La cola registra el éxito, los reintentos y las entregas detenidas. El proveedor puede rechazar reintentos que usen la misma clave de idempotencia dentro de su ventana de deduplicación.
