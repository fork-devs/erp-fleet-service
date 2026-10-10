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