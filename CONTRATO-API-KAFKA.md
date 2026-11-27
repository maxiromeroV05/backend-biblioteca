# Contrato API Kafka — backend Biblioteca

Este documento es la fuente de verdad para el frontend Angular (repositorio aparte).
Todas las rutas nuevas están en el microservicio `biblioteca-admin-kafka` salvo la
auditoría, que está en `biblioteca-api` porque consulta la base de datos.

## 1. Base URL, seguridad y CORS

| Servicio | Base URL | Rutas |
|---|---|---|
| API principal | `http://localhost:8080` | `/api/libros`, `/api/autores`, `/api/usuarios`, `/api/prestamos`, `/api/notificaciones`, **`/api/auditoria`** |
| Admin Kafka | `http://localhost:8081` | **`/api/kafka/**`** |
| Admin Rabbit | `http://localhost:8082` | `/api/rabbit/**` (sin cambios) |

- **Autenticación:** `Authorization: Bearer <access_token>` de Microsoft Entra ID. Los tres microservicios validan el mismo `issuer`, `audience` y `jwk-set-uri`.
- **CORS:** origen permitido `http://localhost:4200`, métodos `GET, POST, PUT, PATCH, DELETE, OPTIONS`, headers `Authorization, Content-Type, Accept`.
- `/api/public/**` es el único grupo público (healthcheck). `/api/**` exige JWT.
- El usuario del JWT se toma del claim `preferred_username` y se guarda en el evento como `usuario`.

## 2. Tópicos Kafka (Kafka UI)

El backend crea los tópicos al arrancar (`KafkaTopicsConfig`) con estos valores,
declarados en `biblioteca-common/src/main/resources/application-common.yml`:

| Tópico | Particiones | Réplicas | `retention.ms` | `cleanup.policy` | Grupo consumidor |
|---|---:|---:|---:|---|---|
| `biblioteca.notificaciones` | 3 | 3 | `604800000` (7 días) | `delete` | `biblioteca-notificaciones` |
| `biblioteca.prestamos` | 3 | 3 | `604800000` (7 días) | `delete` | `biblioteca-prestamos` |
| `biblioteca.notificaciones.DLT` | 3 | 3 | `86400000` (24 h) | `delete` | `biblioteca-dlt` |
| `biblioteca.prestamos.DLT` | 3 | 3 | `86400000` (24 h) | `delete` | `biblioteca-dlt` |

El clúster (`docker-compose.kafka.yml`) tiene **3 nodos ZooKeeper** (quorum, sin KRaft)
y **3 brokers**, con `min.insync.replicas=2`, `acks=all` y `auto.create.topics.enable=false`.
Por eso cada partición aparece con 3 réplicas y 3 ISR en Kafka UI.

### Justificación de los dos tópicos de negocio

1. **`biblioteca.notificaciones`** — avisos al usuario (correo, recordatorios, cambios de estado).
   Es el flujo de mayor volumen y no debe bloquear la gestión de préstamos. Las 3 particiones
   permiten 3 consumidores en paralelo (`listener.concurrency=3`) y la key `id` mantiene el orden
   por mensaje. 3 réplicas toleran la caída de un broker sin perder eventos.
2. **`biblioteca.prestamos`** — ciclo de vida del préstamo: `PRESTAMO_CREADO` y `PRESTAMO_DEVUELTO`.
   Separa las reglas de negocio del sistema de notificaciones: si el consumidor de notificaciones
   falla, el préstamo ya quedó registrado. También usa 3/3 para equilibrar carga y alta disponibilidad.
   El tipo del evento viaja en el campo `tipo`, por lo que el front puede filtrar creaciones y devoluciones.

Los `.DLT` reciben los mensajes que agotan 4 entregas (3 reintentos + intento inicial). El consumidor
del DLT escribe logs con `tópico`, `id` y motivo del fallo. Retención corta porque son evidencia de error,
no datos de negocio.

## 3. DTO exacto que viaja por Kafka

Java: `com.biblioteca.messaging.event.EventoMensajeria` (record).

| Campo | Tipo JSON | Obligatorio | Descripción / origen |
|---|---|---|---|
| `id` | `string` | sí | `mensajeId` de la notificación; se usa también como **key** del mensaje (String) |
| `tipo` | `string` (enum) | sí | `NOTIFICACION`, `TICKET`, `DOCUMENTO`, `PRESTAMO_CREADO`, `PRESTAMO_DEVUELTO` |
| `destinatario` | `string` \| `null` | no | email del usuario; `null` en publicaciones de prueba simples |
| `contenido` | `string` | sí | texto del evento (máx. 1000 caracteres) |
| `fechaEnvio` | `string` | sí | ISO-8601 con offset, p. ej. `2026-11-27T10:15:30.123Z` |
| `correlationId` | `string` \| `null` | no | UUID; se lee del MDC o se genera al publicar |
| `usuario` | `string` \| `null` | no | `preferred_username` del JWT, `null` si la publicación es interna |

Ejemplo de valor (payload del mensaje):

```json
{
  "id": "msg-123",
  "tipo": "PRESTAMO_CREADO",
  "destinatario": "ana@correo.cl",
  "contenido": "Préstamo creado para el libro Clean Code",
  "fechaEnvio": "2026-11-27T10:15:30.123Z",
  "correlationId": "7f1a1f0e-8e2a-4f46-9c7b-1a2b3c4d5e6f",
  "usuario": "ana@correo.cl"
}
```

### Serializers

| Dirección | Key | Value |
|---|---|---|
| Productor | `org.apache.kafka.common.serialization.StringSerializer` | `org.springframework.kafka.support.serializer.JacksonJsonSerializer` |
| Consumidor | `org.apache.kafka.common.serialization.StringDeserializer` | `org.springframework.kafka.support.serializer.JacksonJsonDeserializer` |

Propiedades del value:

```yaml
spring.json.add.type.headers: false
spring.json.value.default.type: com.biblioteca.messaging.event.EventoMensajeria
spring.json.trusted.packages: com.biblioteca.messaging.event
```

El productor usa `acks=all`; el consumidor `isolation.level=read_committed` y confirmación manual
de offsets. No se usa Schema Registry: el contrato es este DTO tipado con Jackson.

## 4. Administración de tópicos y consumer groups

### `GET /api/kafka/topics` → `200`

```json
[
  {
    "name": "biblioteca.notificaciones",
    "partitions": 3,
    "replicationFactor": 3,
    "configs": {}
  }
]
```

El listado devuelve `configs` vacío; el detalle sí las incluye.

### `GET /api/kafka/topics/{name}` → `200` / `404`

```json
{
  "name": "biblioteca.prestamos",
  "partitions": 3,
  "replicationFactor": 3,
  "configs": {
    "retention.ms": "604800000",
    "cleanup.policy": "delete",
    "min.insync.replicas": "2"
  }
}
```

### `POST /api/kafka/topics` → `201`

Request (`TopicRequest`):

```json
{
  "name": "biblioteca.reportes",
  "partitions": 3,
  "replicationFactor": 3,
  "configs": {
    "retention.ms": "604800000",
    "cleanup.policy": "delete"
  }
}
```

`partitions` y `replicationFactor` son opcionales; si se omiten se usan `app.kafka.topic-defaults`
(3 y 3). Nombre permitido: `[A-Za-z0-9._-]+`, máx. 249. Response: `TopicResponse` con el detalle.

### `PUT /api/kafka/topics/{name}` → `200`

Request (`TopicUpdateRequest`); al menos un campo:

```json
{
  "partitions": 6,
  "configs": {
    "retention.ms": "86400000",
    "cleanup.policy": "compact"
  }
}
```

- Kafka solo permite **aumentar** particiones; reducirlas devuelve `400`.
- Un valor `""` o `null` en `configs` elimina el override y vuelve al default del broker.
- El factor de réplicas no se cambia con `PUT` (requiere reassignment).

`DELETE /api/kafka/topics/{name}` → `204`. No se pueden borrar tópicos internos (`__consumer_offsets`).

### `GET /api/kafka/consumer-groups` → `200`

```json
[
  {
    "groupId": "biblioteca-notificaciones",
    "state": "Stable",
    "topics": ["biblioteca.notificaciones"],
    "members": [
      {
        "memberId": "consumer-1",
        "clientId": "consumer-1",
        "host": "/127.0.0.1",
        "assignedPartitions": [
          "biblioteca.notificaciones-0",
          "biblioteca.notificaciones-1"
        ]
      }
    ]
  }
]
```

`GET /api/kafka/consumer-groups/{groupId}` devuelve el mismo shape para un grupo, o `404`.

## 5. Publicación de eventos de prueba

### Opción A — DTO completo (recomendada para el panel del front)

`POST /api/kafka/publish-evento` → `202`

```json
{
  "topic": "biblioteca.prestamos",
  "evento": {
    "id": "msg-123",
    "tipo": "PRESTAMO_CREADO",
    "destinatario": "ana@correo.cl",
    "contenido": "Préstamo creado para el libro Clean Code",
    "fechaEnvio": "2026-11-27T10:15:30.123Z",
    "correlationId": "7f1a1f0e-8e2a-4f46-9c7b-1a2b3c4d5e6f",
    "usuario": "ana@correo.cl"
  }
}
```

Response:

```json
{
  "mensaje": "Evento completo enviado correctamente a Kafka",
  "id": "msg-123",
  "estado": "PENDIENTE"
}
```

El evento se publica **tal cual**: no se rellenan ni se sobreescriben campos. La key es `evento.id`.

### Opción B — publicación rápida

`POST /api/kafka/publish` → `202`

```json
{ "topic": "biblioteca.notificaciones", "contenido": "Hola desde el front" }
```

El backend construye el DTO: `tipo=NOTIFICACION`, `destinatario=null`, `fechaEnvio=now`,
`correlationId` y `usuario` desde el contexto. Response: `{"mensaje":"Mensaje enviado correctamente a Kafka","id":"...","estado":"PENDIENTE"}`.

### Forcear un fallo para la demo del DLT

`POST /api/kafka/publish-invalid` → `202`. Publica en `biblioteca.notificaciones` con `contenido=""`;
el consumidor lo rechaza, agota los reintentos y el mensaje termina en `biblioteca.notificaciones.DLT`.
Response: `{"mensaje":"Mensaje inválido enviado a Kafka (terminará en el DLT)","id":"...","estado":"PENDIENTE"}`.

## 6. Métricas (REST, polling)

`GET /api/kafka/metrics` → `200`. Es una instantánea; el front debe hacer **polling cada 5 segundos**
(valor por defecto de `app.kafka.metrics.polling-interval-seconds`, sobreescribible con
`KAFKA_METRICS_POLLING_SECONDS`). El intervalo se devuelve en la respuesta para que el front no lo
codifique: `pollingIntervalSeconds`.

```json
{
  "generatedAt": "2026-11-27T10:20:00.123Z",
  "pollingIntervalSeconds": 5,
  "topics": [
    {
      "name": "biblioteca.notificaciones",
      "partitions": 3,
      "replicationFactor": 3,
      "totalMessages": 120,
      "eventsPerMinute": 24.0,
      "failures": 2,
      "failuresPerMinute": 0.0
    },
    {
      "name": "biblioteca.prestamos",
      "partitions": 3,
      "replicationFactor": 3,
      "totalMessages": 45,
      "eventsPerMinute": 12.0,
      "failures": 0,
      "failuresPerMinute": 0.0
    }
  ],
  "consumerGroups": [
    {
      "groupId": "biblioteca-notificaciones",
      "state": "Stable",
      "members": 3,
      "committedOffsets": {
        "biblioteca.notificaciones-0": 40,
        "biblioteca.notificaciones-1": 39,
        "biblioteca.notificaciones-2": 39
      },
      "lagByTopic": {
        "biblioteca.notificaciones": 2
      },
      "totalLag": 2
    }
  ],
  "distribution": [
    {
      "groupId": "biblioteca-notificaciones",
      "topic": "biblioteca.notificaciones",
      "partition": 0,
      "committedOffset": 40,
      "endOffset": 41,
      "lag": 1
    }
  ],
  "summary": {
    "eventsPerMinute": 36.0,
    "totalFailures": 2,
    "totalLag": 2
  }
}
```

Notas:

- `totalMessages` y `failures` son suma de offsets **finales** del tópico y de su DLT.
- `eventsPerMinute` / `failuresPerMinute` se calculan comparando con la muestra anterior en memoria;
  la primera respuesta después de reiniciar el servicio devuelve `0.0`.
- `lag = endOffset - committedOffset` por `topico-particion`.
- No hay WS/SSE: es REST con polling. Si el front prefiere SSE, puede envolver este endpoint,
  pero el contrato oficial es este.

## 7. Auditoría filtrable

`GET /api/auditoria` (microservicio API, puerto 8080) → `200`. Todos los filtros son opcionales:

| Query param | Tipo | Ejemplo |
|---|---|---|
| `desde` | ISO-8601 local | `2026-11-27T00:00:00` |
| `hasta` | ISO-8601 local | `2026-11-27T23:59:59` |
| `tipo` | enum | `PRESTAMO_CREADO` |
| `usuario` | string exacto | `ana@correo.cl` |
| `correlationId` | string exacto | `7f1a1f0e-...` |

Ejemplo:

```
GET http://localhost:8080/api/auditoria?desde=2026-11-27T00:00:00&tipo=PRESTAMO_CREADO&correlationId=7f1a1f0e-8e2a-4f46-9c7b-1a2b3c4d5e6f
```

```json
[
  {
    "id": 15,
    "mensajeId": "msg-123",
    "eventoTipo": "PRESTAMO_CREADO",
    "estado": "PROCESADO",
    "usuario": "ana@correo.cl",
    "correlationId": "7f1a1f0e-8e2a-4f46-9c7b-1a2b3c4d5e6f",
    "destinatario": "ana@correo.cl",
    "contenido": "Préstamo creado para el libro Clean Code",
    "canal": "biblioteca.prestamos",
    "fechaCreacion": "2026-11-27T10:15:30.100",
    "fechaProcesamiento": "2026-11-27T10:15:30.900",
    "error": null
  }
]
```

Un `tipo` no válido devuelve `400` indicando los valores permitidos.

## 8. Compatibilidad confirmada

- **`/api/rabbit/**` sigue igual**: colas, exchanges, bindings, `publish` y `publish-invalid` en `http://localhost:8082`.
- **`/api/notificaciones` sigue igual**: `GET` lista `NotificacionResponse` y `POST` publica en Kafka en `http://localhost:8080`.
- Los nuevos endpoints usan **el mismo JWT de Entra** y **el mismo CORS** `http://localhost:4200`.

## 9. Flujo de demostración extremo a extremo

1. **Levantar infraestructura**

   ```bash
   docker compose -f docker-compose.yml up -d                 # RabbitMQ
   docker compose -f docker-compose.kafka.yml up -d           # 3 ZooKeeper + 3 brokers
   docker compose -f docker-compose.kafka-ui.yml up -d        # Kafka UI en :8085
   docker compose -f docker-compose.backend.yml up -d         # API + admins
   ```

   En Kafka UI (`http://localhost:8085`) deben verse 3 brokers, los 4 tópicos con 3 particiones
   y 3 réplicas, y los consumer groups.

2. **Obtener token Entra** con el mismo flujo de login del front y usarlo como `Bearer`.

3. **Ver tópicos y grupos**

   ```bash
   curl -H "Authorization: Bearer $TOKEN" localhost:8081/api/kafka/topics
   curl -H "Authorization: Bearer $TOKEN" localhost:8081/api/kafka/consumer-groups
   curl -H "Authorization: Bearer $TOKEN" -X POST localhost:8081/api/kafka/topics \
     -H "Content-Type: application/json" \
     -d '{"name":"biblioteca.demo","partitions":3,"replicationFactor":3,"configs":{"retention.ms":"604800000","cleanup.policy":"delete"}}'
   ```

4. **Publicar el DTO exacto**

   ```bash
   curl -H "Authorization: Bearer $TOKEN" -X POST localhost:8081/api/kafka/publish-evento \
     -H "Content-Type: application/json" \
     -d '{"topic":"biblioteca.prestamos","evento":{"id":"demo-1","tipo":"PRESTAMO_CREADO","destinatario":"ana@correo.cl","contenido":"Demo préstamo","fechaEnvio":"2026-11-27T10:15:30Z","correlationId":"demo-corr-1","usuario":"ana@correo.cl"}}'
   ```

   En Kafka UI: mensaje en `biblioteca.prestamos`, key `demo-1`, 7 campos en el value.

5. **Ver consumo y métricas**

   ```bash
   curl -H "Authorization: Bearer $TOKEN" localhost:8081/api/kafka/metrics
   ```

   Repetir cada 5 s: `eventsPerMinute` sube, `committedOffsets` avanza y `totalLag` baja.

6. **Probar el DLT**

   ```bash
   curl -H "Authorization: Bearer $TOKEN" -X POST localhost:8081/api/kafka/publish-invalid
   ```

   Esperar los reintentos; luego ver `biblioteca.notificaciones.DLT` en Kafka UI y
   `failures` > 0 en `/api/kafka/metrics`. Los logs del administrador y de la API muestran
   el motivo del fallo.

7. **Cerrar el círculo con auditoría**

   `POST /api/kafka/publish-evento` es una herramienta de prueba del administrador: publica el DTO,
   pero **no guarda auditoría** porque ese microservicio no usa base de datos. Para ver el ciclo
   completo usa el productor de negocio:

   ```bash
   curl -H "Authorization: Bearer $TOKEN" -X POST localhost:8080/api/notificaciones \
     -H "Content-Type: application/json" \
     -d '{"destinatario":"ana@correo.cl","contenido":"Demo auditoría"}'

   curl -H "Authorization: Bearer $TOKEN" \
     "localhost:8080/api/auditoria?tipo=NOTIFICACION"
   ```

   La fila más reciente muestra `correlationId`, `usuario`, `canal=biblioteca.notificaciones`,
   `estado=PROCESADO` y `fechaProcesamiento`. Ese `correlationId` se puede copiar y reutilizar
   como filtro exacto: `/api/auditoria?correlationId=<valor>`.

8. **Rabbit sin cambios**

   ```bash
   curl -H "Authorization: Bearer $TOKEN" localhost:8082/api/rabbit/queues
   curl -H "Authorization: Bearer $TOKEN" localhost:8080/api/notificaciones
   ```
