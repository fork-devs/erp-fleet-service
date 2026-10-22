# Documentación de Arquitectura e Integración — `erp-fleet-service`

**Microservicio:** Gestión de Clientes y Parque Automotor (Customer & Fleet)  
**Sistema:** DriveOS ERP SaaS  
**Organización:** ForkDevs — Universidad Peruana de Ciencias Aplicadas (UPC)  
**Curso:** Fundamentos de Arquitectura de Software (1ASI0657)  

---

## 1. Visión General del Microservicio

El microservicio `erp-fleet-service` es el componente responsable del catálogo maestro de **Clientes** y **Vehículos** para los talleres mecánicos que operan sobre la plataforma DriveOS ERP SaaS. 

Actúa como el repositorio centralizado de activos automotrices y datos de contacto de clientes, sirviendo como fuente primaria de datos para procesos posteriores de cotización, diagnósticos OBD-II, órdenes de trabajo y facturación electrónica.

```
                    ┌─────────────────────────┐
                    │   DriveOS Frontend      │
                    │   (Angular / Mobile)    │
                    └───────────┬─────────────┘
                                │ HTTP / REST (JWT Bearer)
                                ▼
                    ┌─────────────────────────┐
                    │    erp-api-gateway      │  Puerto 8080
                    │  (Edge & Token Verify)  │
                    └───────────┬─────────────┘
                                │ HTTP / REST (Inyecta X-Tenant-ID, X-User-ID)
                                ▼
        ┌─────────────────────────────────────────────────┐
        │               erp-fleet-service                 │  Puerto 8083
        │        (Clean Architecture / Spring Boot)       │
        └───────┬─────────────────────────────┬───────────┘
                │                             │
       JDBC/SQL │ (Postgres RLS)      HTTP/REST│ Circuit Breaker
                ▼                             ▼
   ┌───────────────────────┐     ┌────────────────────────┐
   │     erp_fleet_db      │     │  erp-integration-hub   │  Puerto 8085
   │     (Puerto 5435)     │     │ (RENIEC / SUNAT / MTC) │
   └───────────────────────┘     └────────────────────────┘
```

---

## 2. Arquitectura de Software (Clean Architecture)

El servicio sigue estrictamente los principios de **Clean Architecture** (Arquitectura Limpia) y **Domain-Driven Design (DDD)**, estructurado en cuatro capas concéntricas donde las dependencias siempre apuntan hacia adentro:

```
      interfaces.rest (Controllers, DTOs, Exception Handlers)
                 │
                 ▼
      application (Use Cases, Services, Commands, Results)
                 │
                 ▼
      domain (Entities, Value Objects, Ports, Domain Exceptions)
                 ▲
                 │
      infrastructure (JPA Entities, Adapters, Config, Repositories)
```

### Estructura de Paquetes (`com.erp.fleet`):

```
com.erp.fleet/
├── domain/                                  # CAPA CORE: Reglas de negocio puras (sin Spring)
│   ├── exception/                           # Excepciones de negocio (CustomerNotFound, InvalidPlate, etc.)
│   ├── model/                               # Entidades y Agregados (Customer, Vehicle) y Enums
│   ├── port/                                # Puertos de salida hacia infraestructura (CustomerRepositoryPort, VehicleRepositoryPort)
│   └── valueobject/                         # Inmutables con validación (TenantId, CustomerId, VehicleId, Plate, DocNumber)
│
├── application/                             # CAPA DE APLICACIÓN: Orquestación de casos de uso
│   ├── dto/
│   │   ├── command/                         # Comandos inmutables (RegisterCustomerCommand, etc.)
│   │   └── result/                          # Resultados inmutables hacia las capas externas
│   ├── service/                             # Implementaciones transaccionales de casos de uso
│   └── usecase/                             # Interfaces de casos de uso (puertos de entrada)
│
├── infrastructure/                          # CAPA DE INFRAESTRUCTURA: Detalles tecnológicos
│   ├── config/                              # Seguridad stateless, CORS, OpenAPI, cableado Spring
│   └── persistence/
│       ├── adapter/                         # Adaptadores que implementan los puertos de dominio
│       ├── entity/                          # Entidades JPA mapeadas a tablas SQL
│       ├── mapper/                          # Mappers bidireccionales Dominio <-> Entidad JPA
│       └── repository/                      # Repositorios Spring Data JPA
│
└── interfaces/rest/                         # CAPA DE ENTRADA: Controladores HTTP
    ├── advice/                              # Manejo global de excepciones (@RestControllerAdvice)
    ├── controller/                          # CustomerController, VehicleController
    └── dto/
        ├── request/                         # Cuerpos de petición con validaciones (@Valid)
        └── response/                        # Respuestas HTTP presentadas al cliente
```

---

## 3. Modelo de Base de Datos y Aislamiento Multi-Tenant

### 3.1. Base de Datos Dedicada
Cumpliendo el principio de **Database-per-Service**, `erp-fleet-service` posee su propia base de datos física/lógica:
* **Motor:** PostgreSQL 16
* **Nombre de BD:** `erp_fleet_db`
* **Puerto:** 5435
* **Herramienta de migración:** Flyway (`V1__create_customers_table.sql`, `V2__create_vehicles_table.sql`, `V3__rls_y_roles_fleet.sql`, `V4__validation_result_columns.sql`)
* **Rol de ejecución:** `erp_fleet_app`, que **no** es propietario de las tablas y **no** tiene `BYPASSRLS`. El propietario (`erp_fleet_owner`) lo usan solo las migraciones.
* **Rol de plataforma:** `erp_fleet_platform`, con `BYPASSRLS`, usado **únicamente** por el barrido de revalidación, que recorre los talleres con registros pendientes. Su credencial vive separada de la de la aplicación.

### 3.2. Aislamiento con Row Level Security (RLS)
Para garantizar la estricta separación de datos entre diferentes talleres (inquilinos):
1. Todas las tablas incluyen una columna obligatoria `tenant_id UUID NOT NULL`.
2. Las políticas de PostgreSQL RLS aíslan cada fila automáticamente. La RLS se **habilita y se fuerza**: `FORCE` por sí solo no la habilita, solo obliga a que la política se aplique también al propietario, que es el rol que la eludía.
   ```sql
   ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
   ALTER TABLE customers FORCE  ROW LEVEL SECURITY;
   CREATE POLICY customer_tenant_isolation ON customers
       USING      (tenant_id = current_setting('app.tenant_id')::uuid)
       WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);

   ALTER TABLE vehicles ENABLE ROW LEVEL SECURITY;
   ALTER TABLE vehicles FORCE  ROW LEVEL SECURITY;
   CREATE POLICY vehicle_tenant_isolation ON vehicles
       USING      (tenant_id = current_setting('app.tenant_id')::uuid)
       WITH CHECK (tenant_id = current_setting('app.tenant_id')::uuid);
   ```
3. El contexto de taller se lee **sin** el flag `missing_ok`: sin contexto, la consulta falla de forma explícita en lugar de devolver cero filas en silencio. El aislamiento no se relaja calladamente.
4. El contexto lo establece el **principal autenticado**, nunca la fila que se opera: si lo fijase la fila, la comprobación de la política sería siempre satisfactoria y el aislamiento no aislaría nada. Se aplica con `SET LOCAL` **dentro** de la transacción, con el orden de advice declarado (el interceptor transaccional en el orden 0 y el aspecto de RLS en el 1).
5. La aplicación conecta con `erp_fleet_app`, que no es propietario de las tablas. Los repositorios Spring Data filtran además por `tenant_id` como segunda barrera, no como única.

### 3.3. Relación Lógica entre Entidades
No existen claves foráneas (`FOREIGN KEY`) cruzadas entre microservicios:
* La tabla `vehicles` almacena la clave `customer_id UUID`, representando una **referencia lógica**.
* Esto permite evolucionar o migrar el servicio de clientes de forma independiente sin acoplamientos de esquemas relacionales.

---

## 4. Conexiones e Integraciones en el Ecosistema Backend

A continuación se detalla cómo se integra `erp-fleet-service` con los demás componentes del ecosistema DriveOS:

### 4.1. Conexión Entrada: `erp-api-gateway` → `erp-fleet-service`
* **Protocolo:** HTTP / REST síncrono.
* **Seguridad Perimetral:** El API Gateway valida el JWT emitido por `erp-auth-service`. Si el token es válido:
  1. Extrae los claims del token.
  2. Inyecta los siguientes headers HTTP hacia `erp-fleet-service`:
     * `X-Tenant-ID`: UUID del taller autenticado.
     * `X-User-ID`: UUID del usuario emisor.
     * `X-User-Roles`: Roles asignados (ej. `MECANICO`, `ADMIN`).
* **Seguridad Interna:** `erp-fleet-service` opera de manera *stateless* y **valida el token de acceso** contra el conjunto de claves del emisor (`/.well-known/jwks.json`). El taller se deriva **exclusivamente** del claim `tenant_id` del token verificado: la cabecera `X-Tenant-ID` es una **pista**, no una autoridad, y el servicio la contrasta con el token y rechaza la petición cuando no coinciden. Confiar en la red interna no es una autorización: el servicio es alcanzable directamente, y quien lo alcance podría elegir el taller.

### 4.2. Conexión Salida: `erp-fleet-service` → `erp-integration-hub` (ACL)
* **Objetivo:** Validación de autenticidad de documentos (DNI/RUC con RENIEC/SUNAT) y antecedentes de placa/VIN (MTC).
* **Flujo Asíncrono no bloqueante:**
  1. Cuando un usuario registra un cliente o vehículo en `erp-fleet-service`, el registro se persiste inmediatamente en la base de datos con `validation_status = 'PENDING'`.
  2. `erp-fleet-service` consulta a `erp-integration-hub` por **HTTPS**, con su **propia clave de servicio**; el hub le concede el rol acotado `ROLE_FLEET`.
  3. Si el hub declara el documento o la placa válidos, el registro pasa a `VALID` y se guardan los datos que informa.
  4. Si el hub informa una discrepancia, el registro pasa a `OBSERVED`: el taller tiene que revisarlo. **No** es un fallo de la validación.
  5. Si el hub no está disponible, la operación del taller **no se detiene**: el registro permanece en `PENDING`, se cuenta el intento y se programa el siguiente. El **Circuit Breaker** de Resilience4j vive en fleet, porque el degradado es una decisión de quien llama: un circuito dentro del hub solo podría negarse a responder, no comunicar al que llama que debe degradar.
  6. Un **barrido programado** revalida los registros pendientes cuya espera ha vencido, con un **límite por taller** para no agotar la cuota de validación del hub, que se comparte entre talleres.
  7. Los resultados correctos se sirven con **caché** (Redis): un RUC 30 días y una placa 7 días.

### 4.3. Conexión Salida / Consumo: `erp-work-order-service` → `erp-fleet-service`
* **Objetivo:** Creación y asociación de órdenes de servicio automotriz.
* **Interacción:**
  * Al iniciar una orden de reparación, `erp-work-order-service` consulta a `erp-fleet-service`:
    * `GET /api/v1/vehicles/{id}` → Para validar marca, modelo, año y VIN del auto ingresado al taller.
    * `GET /api/v1/customers/{id}` → Para validar los datos de facturación del propietario.

---

## 5. Especificación de la API REST

Todos los endpoints están documentados bajo el estándar OpenAPI 3.0 / Swagger en:  
👉 **`http://localhost:8083/swagger-ui/index.html`**

### Matriz de Endpoints

Todos los endpoints se publican bajo **`/api/v1/fleet`**, que es el `path-prefix`
que el gateway reenvía **sin quitarlo**: el prefijo del gateway *es* la ruta
del servicio. Un endpoint fuera de ese prefijo es inalcanzable desde el
gateway.

Todos exigen un **token de acceso verificado**. El taller se deriva del token;
la cabecera `X-Tenant-ID` es una pista que se contrasta con él.

| Método | Endpoint | Descripción | Rol |
|---|---|---|---|
| **POST** | `/api/v1/fleet/customers` | Registrar un nuevo cliente en el taller | `SUPERADMIN`, `ADMIN_TALLER`, `ASESOR` |
| **GET** | `/api/v1/fleet/customers` | Listar todos los clientes del taller | + `MECANICO` |
| **GET** | `/api/v1/fleet/customers/{id}` | Obtener detalle de cliente por ID | + `MECANICO` |
| **PUT** | `/api/v1/fleet/customers/{id}` | Actualizar datos de contacto del cliente | `SUPERADMIN`, `ADMIN_TALLER`, `ASESOR` |
| **DELETE** | `/api/v1/fleet/customers/{id}` | Desactivar cliente (baja lógica; no admite vehículos asociados) | `SUPERADMIN`, `ADMIN_TALLER` |
| **POST** | `/api/v1/fleet/customers/{id}/validate` | Reintentar la validación pendiente del cliente | `SUPERADMIN`, `ADMIN_TALLER`, `ASESOR` |
| **POST** | `/api/v1/fleet/vehicles` | Registrar nuevo vehículo asociado a un cliente | `SUPERADMIN`, `ADMIN_TALLER`, `ASESOR` |
| **GET** | `/api/v1/fleet/vehicles` | Listar todos los vehículos registrados en el taller | + `MECANICO` |
| **GET** | `/api/v1/fleet/vehicles/{id}` | Obtener vehículo por ID | + `MECANICO` |
| **GET** | `/api/v1/fleet/vehicles/customer/{customerId}` | Listar flota vehicular de un cliente específico | + `MECANICO` |
| **PUT** | `/api/v1/fleet/vehicles/{id}` | Actualizar datos del vehículo (año, modelo, VIN) | `SUPERADMIN`, `ADMIN_TALLER`, `ASESOR` |
| **POST** | `/api/v1/fleet/vehicles/{id}/validate` | Reintentar la validación pendiente del vehículo | `SUPERADMIN`, `ADMIN_TALLER`, `ASESOR` |

**Errores.** El cuerpo del error es un `ProblemDetail` (RFC 9457) con un código
estable por familia (`code`) y el taller (`tenant_id`). El cliente puede
distinguirlas sin adivinar:

| Código | HTTP | Significado |
|---|---|---|
| `FLEET_DOC_DUPLICADO` | 409 | Ya existe un cliente con ese documento **en el taller** |
| `FLEET_PLATE_DUPLICADO` | 409 | Ya existe un vehículo con esa placa **en el taller** |
| `FLEET_CUSTOMER_HAS_VEHICLES` | 409 | El cliente tiene vehículos y no se puede dar de baja |
| `FLEET_CUSTOMER_NOT_FOUND` | 404 | El cliente no existe en el taller |
| `FLEET_VEHICLE_NOT_FOUND` | 404 | El vehículo no existe en el taller |
| `FLEET_NOT_FOUND` | 404 | La ruta no existe |
| `FLEET_ACCESS_DENIED` | 403 | El principal no tiene el rol que exige el endpoint |
| `FLEET_UNAUTHENTICATED` | 401 | La petición no trae un token verificado |
| `FLEET_VALIDATION_ERROR` | 400 | El cuerpo de la petición no es válido |
| `FLEET_INTERNAL_ERROR` | 500 | Error no previsto (registrado con su traza) |

---

## 6. Guía de Despliegue y Puesta en Marcha

### Prerrequisitos
* Java 21 JDK instalado.
* Docker Desktop activo.

### Pasos de Ejecución Local
```bash
# 1. Posicionarse en la raíz del microservicio
cd "C:\Users\ASUS\Documents\FUNDAMENTOS\informefinal\Nueva carpeta\erp-fleet-service"

# 2. Levantar la base de datos PostgreSQL en Docker
docker compose up -d

# 3. Compilar y ejecutar la aplicación mediante el Maven Wrapper
.\mvnw.cmd spring-boot:run
```

El servicio estará listo para recibir peticiones en el puerto **`8083`**.
