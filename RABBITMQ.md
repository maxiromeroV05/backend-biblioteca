# RabbitMQ en Backend Biblioteca

## Levantar el clúster (2 nodos)

```bash
cp .env.example .env          # opcional: cambia usuario/clave
docker compose up -d
docker exec rabbit1 rabbitmqctl cluster_status    # deben aparecer rabbit@rabbit1 y rabbit@rabbit2
```

Panel web: http://localhost:15672 (nodo 1) y http://localhost:15673 (nodo 2).
Usuario por defecto: `biblioteca` / `biblioteca123` (no se usa guest/guest).

Opcional, para que las colas sobrevivan si cae un nodo (mirroring clásico):

```bash
docker exec rabbit1 rabbitmqctl set_policy ha-biblioteca "^(notificaciones|tickets|documentos)\." '{"ha-mode":"all"}' --apply-to queues
```

## Arrancar el backend

```bash
./mvnw -pl biblioteca-api spring-boot:run
```

Al arrancar, `biblioteca-api` crea en RabbitMQ la topología definida en `application-common.yml`:

| Exchange | Tipo | Routing key | Cola | DLQ |
|---|---|---|---|---|
| `biblioteca.topic` | topic | `notificacion.#` | `notificaciones.queue` | `notificaciones.dlq` |
| `biblioteca.direct` | direct | `ticket.crear` | `tickets.queue` | `tickets.dlq` |
| `biblioteca.direct` | direct | `documento.generar` | `documentos.queue` | `documentos.dlq` |
| `biblioteca.dlx` | direct | `*.dead` | (las 3 DLQ) | |

Para cambiar cualquier nombre basta editar `app.rabbit` en `biblioteca-common/src/main/resources/application-common.yml`.

## Eventos de negocio

- Crear un préstamo: notificación (`notificacion.prestamo.creado`) + ticket.
- Devolver un préstamo: notificación (`notificacion.prestamo.devuelto`) + comprobante (documento).
- Si RabbitMQ no está disponible, el préstamo igual se crea o devuelve.

## Manejo de errores en consumidores (ACK manual)

| Situación | Acción |
|---|---|
| Se procesa bien | ACK |
| Mensaje inválido (contenido vacío, sin id) | NACK sin requeue → DLQ (log `ERROR`) |
| Error recuperable (ej. falla la BD) | Reintento (header `x-retry-count`) hasta `app.rabbit.retry.max-attempts`; luego NACK → DLQ |

`app.rabbit.dlq-listener.enabled` (por defecto `true`, variable `RABBIT_DLQ_LISTENER_ENABLED`) mantiene activo un listener que consume las DLQ y registra cada mensaje muerto en los logs (`ERROR`, con la cola de origen y el motivo). Con `false` los mensajes se quedan en la DLQ para verlos desde el panel; en ese caso el log lo deja igualmente el consumidor original al rechazarlos.

## API de administración (requiere JWT)

El microservicio `biblioteca-admin-rabbit` escucha en `http://localhost:8082`.

| Método | Ruta | Descripción |
|---|---|---|
| GET / POST | `/api/rabbit/queues` | Listar / crear cola |
| DELETE | `/api/rabbit/queues/{name}` | Eliminar cola |
| GET / POST | `/api/rabbit/exchanges` | Listar / crear exchange |
| DELETE | `/api/rabbit/exchanges/{name}` | Eliminar exchange |
| GET / POST | `/api/rabbit/bindings` | Listar / crear binding |
| DELETE | `/api/rabbit/bindings?exchange=&queue=&routingKey=` | Eliminar binding |
| POST | `/api/rabbit/publish` | Publicar mensaje `{exchange, routingKey, payload}` |
| POST | `/api/rabbit/publish-invalid` | Publicar mensaje inválido (termina en la DLQ) |
| GET | `/api/notificaciones` | Estado de las tareas asíncronas |

Ejemplo de cuerpo para crear una cola con DLQ:

```json
{ "name": "reportes.queue", "durable": true,
  "deadLetterExchange": "biblioteca.dlx", "deadLetterRoutingKey": "notificaciones.dead" }
```

Los errores de validación devuelven HTTP 400 con el detalle por campo.
