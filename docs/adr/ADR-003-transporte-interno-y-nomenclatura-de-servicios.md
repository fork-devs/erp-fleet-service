# ADR-003 — Transporte interno de fleet y nomenclatura canónica de los servicios

- **Estado:** Aceptado
- **Fecha:** 2026-10-21
- **Change:** `add-fleet-service`
- **Ámbito:** `erp-fleet-service`, `erp-api-gateway` y `erp-integration-hub`

## Contexto

`doc-microservices.md` §2 describe `erp-fleet-service` como un servicio Java
Spring Boot que se comunica por «REST + gRPC», y §4 reserva la comunicación
síncrona interna por gRPC —con `deadline ≤ 500 ms`, reintento único y
circuit-breaker— para «solo lecturas o reservas idempotentes», y nombra
`fleet→hub` entre ellas.

El **diagrama C4 de contenedores**, en cambio, dibuja esa misma interacción
como HTTPS en sus tres vistas:

```
Rel(gateway, fleet, "Reenvia [HTTPS]")
Rel(fleet, hub, "Valida via hub [HTTPS]")
Rel(workorder, inventory, "Reserva stock [gRPC]")
```

Es decir: el gRPC del spec queda asignado a `gateway→auth` («Valida JWT
[gRPC/HTTPS]») y a `work-order→inventory`, la reserva idempotente de la saga.
El par `fleet→hub` aparece **siempre** como HTTPS.

El reporte de estado dejó esta decisión pendiente de forma explícita, en la
acción 33 del Sprint 2: *«Resolver gRPC-vs-REST en un ADR antes de construir
`fleet-service`»*, con ASR QA-7.

Hay un segundo punto abierto. El mismo reporte (§2, nota de nomenclatura)
advierte de que conviven dos nombres para los mismos servicios: `core-service`
y `operations-service`, de un lado, y `erp-fleet-service` y
`erp-work-order-service`, del otro, y pide *«fijar una sola nomenclatura en un
ADR antes de construir los 7 restantes»*. El spec canónico
(`doc-microservices.md` §2) es el que define `erp-fleet-service`.

## Decisión

**1. La llamada de `erp-fleet-service` al `erp-integration-hub` es REST sobre
HTTPS.** fleet consulta la validación de identidad y de vehículo contra
`GET /api/v1/integrations/validate/ruc/{ruc}` y
`GET /api/v1/integrations/validate/placa/{placa}`.

**2. La nomenclatura canónica de los servicios es la del spec de
`doc-microservices.md` §2**: `erp-fleet-service`, `erp-inventory-service`,
`erp-work-order-service` y `erp-billing-service`. Los nombres `core-service` y
`operations-service` quedan descartados.

**3. La API de fleet se publica bajo el prefijo `/api/v1/fleet`**, que es el
`path-prefix` que el gateway reenvía sin quitarlo, de modo que el prefijo del
gateway y la ruta del servicio son la misma cosa.

## Motivo

**Sobre el transporte.** El hub ya expone la validación por HTTPS, con su
propia caché y su límite por taller; y el `doc/arquitectura-y-conexiones.md`
§4.2 del propio servicio ya describe el flujo por HTTPS. Añadir gRPC para
esa llamada habría obligado a introducir un contrato `.proto` nuevo, un
servidor gRPC en el hub y tooling nuevo en los dos lados, a cambio de nada:
la lectura que se necesita ya existe en REST.

El argumento a favor de gRPC —tipos fuertes y menos ceremonia de serialización—
tampoco pesa aquí: el payload de la validación es un JSON pequeño y de forma
estable, y el plazo de 500 ms del spec se cumple de sobra porque la llamada
se sirve con caché y, sobre todo, porque **la validación no bloquea el
registro**: el alta se confirma con estado `PENDING` y la validación se
intenta después. La latencia de la validación no está en el camino crítico
del taller.

**Sobre la nomenclatura.** `doc-microservices.md` §2 es el catálogo canónico
del sistema, y es el que usan el gateway (su tabla de rutas ya se llama
`fleet-service` y apunta a `erp-fleet-service`), el contexto C4 y el modelo
de datos. Adoptar los nombres del catálogo evita que convivan dos vocabularios
en la documentación, en el código y en los nombres de contenedor.

**Sobre el prefijo.** La convención de la plataforma es que el prefijo del
gateway **es** la ruta del servicio: `/api/v1/auth`, `/api/v1/tenants`,
`/api/v1/plans`, `/api/v1/integrations/**`. fleet publicaba sus rutas sin el
prefijo, de modo que toda petición que llegaba por el gateway terminaba en
`404`. La alternativa —quitar el prefijo en el gateway— rompería la convención
y dejaría las rutas de fleet (`/api/v1/customers`) sin espacio de nombres,
colisionando con las de cualquier otro servicio, incluido el que consumirá
estos mismos datos: `erp-work-order-service`.

## Alternativas descartadas

**gRPC para `fleet→hub`.** Se descartó porque el gRPC del spec está asignado
a otras dos interacciones —la validación de JWT en el borde y la reserva de
stock de la saga—, porque el hub ya sirve la validación por HTTPS, y porque
la latencia de la validación no está en el camino crítico del taller.

**Que el gateway quite el prefijo antes de reenviar.** Se descartó porque
rompe la convención de la plataforma, deja las rutas de fleet sin espacio de
nombres frente a las de todos los demás servicios, y obliga a tocar el
gateway —el componente que hoy está roto en esta ruta— para acomodar a un
servicio nuevo.

**Adoptar `core-service` y `operations-service`.** Se descartó porque el
catálogo canónico de `doc-microservices.md` §2 define los otros nombres, y
porque el gateway, el contexto C4 y el modelo de datos ya usan los del
catálogo.

## Consecuencias

**Positivas.**

- El gRPC queda libre para la reserva de stock de la saga, donde sí aporta:
  reserva idempotente, deadline corto y semántica de operación clara.
- fleet deja de depender de tooling gRPC y de un servidor nuevo en el hub.
- El prefijo `/api/v1/fleet` hace que la ruta del servicio y la del gateway
  coincidan, y elimina la clase de fallo «404 porque el prefijo no coincide».
- Un solo vocabulario de nombres de servicio en el código y en la documentación.

**Negativas.**

- La validación de identidad y de vehículo es una llamada HTTP con JSON, sin
  las garantías de tipo de un contrato `.proto`. El coste se asume porque el
  payload es pequeño y estable, y la caché de Redis reduce el número de
  llamadas a una por taller y por clave.
- Cambiar el prefijo de las rutas de fleet es un cambio incompatible para
  quien consume el servicio por su URL actual.hoy no hay consumidor
  posible por el gateway, porque las rutas anteriores no eran alcanzables.
- Se pierde la verificación de contrato que gRPC daría entre ambos lados. Se
  compensa con el test de equivalencia del cliente del conjunto de claves y
  con el test de contrato de API de fleet.

**Neutrales.**

- El cambio de prefijo afecta solo a fleet. El gateway no cambia de
  contrato, salvo el plazo de la ruta, que pasa de 10 s a 500 ms.

## Alcance de la decisión posterior

- La decisión de **REST o gRPC para `work-order→fleet`** (la lectura de datos
  de cliente y vehículo desde la orden de trabajo) **no** se toma aquí: se
  decide en el change de `erp-work-order-service`. La documentación del
  servicio de fleet la describe hoy por REST (`GET /api/v1/vehicles/{id}`,
  `GET /api/v1/customers/{id}`), coherente con este ADR, pero la decisión
  formal es de quien va a consumir.
- La decisión de **gRPC para `billing→hub`** tampoco se toma aquí, y sigue
  siendo la alternativa natural si la emisión de comprobantes necesita la
  latencia y el tipado que el spec le atribuye.
- Este ADR **no** decide quién posee el estado fiscal de los comprobantes:
  eso es materia del ADR-002.
