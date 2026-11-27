# Backend Biblioteca

Backend académico de una biblioteca hecho con **varios microservicios Spring Boot**. Expone una API REST, guarda datos en PostgreSQL cloud (o H2 en local), valida JWT de Microsoft Entra ID y publica eventos en **RabbitMQ** y **Kafka**. El frontend Angular se entrega en otro repositorio.

## Qué hay en el proyecto

| Módulo | Función | Puerto |
|---|---|---:|
| `biblioteca-api` | API de libros, autores, usuarios, préstamos, notificaciones y auditoría; productores/consumidores de mensajería | 8080 |
| `biblioteca-admin-kafka` | Administración de tópicos, consumer groups, publicación de prueba y métricas | 8081 |
| `biblioteca-admin-rabbit` | Administración de colas, exchanges y bindings | 8082 |
| `biblioteca-common` | Entidades, DTOs, seguridad JWT/CORS y configuración compartida | — |

Archivos de infraestructura:

| Archivo | Levanta |
|---|---|
| `docker-compose.yml` | RabbitMQ: 2 nodos |
| `docker-compose.kafka.yml` | Kafka: 3 ZooKeeper + 3 brokers |
| `docker-compose.kafka-ui.yml` | Kafka-UI |
| `docker-compose.backend.yml` | Los 3 microservicios Spring Boot |

Documentación de apoyo: [CONTRATO-API-KAFKA.md](CONTRATO-API-KAFKA.md), [RABBITMQ.md](RABBITMQ.md) y [GUIA-EVIDENCIAS-EP5.md](GUIA-EVIDENCIAS-EP5.md) (fotos, referencias `EV-xx` y rúbrica; también hay versión Word).

## Qué hace

- Gestiona el catálogo, usuarios y préstamos por REST.
- Al crear o devolver un préstamo publica eventos en RabbitMQ y Kafka; si la mensajería falla, el préstamo igual queda registrado.
- Publica notificaciones en `biblioteca.notificaciones` mediante `POST /api/notificaciones`.
- Permite administrar tópicos, configuraciones y consumer groups de Kafka por API o por Kafka-UI.
- Expone métricas de Kafka (offsets, lag, distribución, eventos/minuto y fallos) y auditoría filtrable por fecha, tipo, usuario y `correlationId`.
- Registra los mensajes no entregados en `DLQ` (RabbitMQ) y `DLT` (Kafka).

## Requisitos

- Java 21.
- Docker con Docker Compose.
- Maven no es necesario: se usa el wrapper `mvnw`. En Windows usa `.\mvnw.cmd` donde se vea `./mvnw`.

## Cómo echarlo a andar

Desde la raíz del proyecto, en este orden:

```bash
# 1. Variables de entorno
copy .env.example .env          # Windows
# cp .env.example .env          # macOS / Linux

# 2. Infraestructura
docker compose -f docker-compose.yml up -d
docker compose -f docker-compose.kafka.yml up -d
docker compose -f docker-compose.kafka.yml ps       # esperar a que los 6 contenedores estén "healthy"
docker compose -f docker-compose.kafka-ui.yml up -d

# 3. Microservicios
docker compose -f docker-compose.backend.yml up -d --build

# 4. Comprobación
curl http://localhost:8080/api/public/health        # {"status":"UP","service":"biblioteca-backend"}
```

Los clústeres deben arrancar antes que los microservicios porque `docker-compose.backend.yml` usa sus redes externas.

URLs:

| Servicio | URL |
|---|---|
| API principal | http://localhost:8080 |
| Admin Kafka | http://localhost:8081 |
| Admin Rabbit | http://localhost:8082 |
| Kafka-UI | http://localhost:8085 |
| RabbitMQ panel | http://localhost:15672 (`biblioteca` / `biblioteca123`) |

Logs y apagado:

```bash
docker compose -f docker-compose.backend.yml logs -f biblioteca-api
docker compose -f docker-compose.backend.yml down
docker compose -f docker-compose.kafka-ui.yml down
docker compose -f docker-compose.kafka.yml down
docker compose down
```

Agregar `-v` a `down` borra también los datos de los volúmenes.

## Ejecución sin contenedores de los microservicios

La infraestructura sigue siendo necesaria para RabbitMQ/Kafka:

```bash
./mvnw clean verify
./mvnw -pl biblioteca-api spring-boot:run
./mvnw -pl biblioteca-admin-kafka spring-boot:run
./mvnw -pl biblioteca-admin-rabbit spring-boot:run
```

Para probar sin JWT, solo en local:

```bash
./mvnw -pl biblioteca-api spring-boot:run -Dspring-boot.run.profiles=dev
```

Perfiles:

| Perfil | Base de datos | Seguridad |
|---|---|---|
| Por defecto | H2 en memoria | JWT obligatorio |
| `cloud` | PostgreSQL (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) | JWT obligatorio |
| `dev` | H2 en memoria | Sin autenticación |

## Configuración mínima

Todas las variables están documentadas en `.env.example`. Las principales son:

| Variable | Uso |
|---|---|
| `SPRING_PROFILES_ACTIVE=cloud` | Activa PostgreSQL cloud; sin ella se usa H2 |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexión a PostgreSQL |
| `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | Credenciales de RabbitMQ |
| `KAFKA_BOOTSTRAP_SERVERS` | Brokers Kafka |
| `CORS_ALLOWED_ORIGINS` | Por defecto `http://localhost:4200` |
| `KAFKA_METRICS_POLLING_SECONDS` | Intervalo de `/api/kafka/metrics`, por defecto `5` |
| `KAFKA_ENABLED=false` | Desactiva tópicos, consumo y publicación Kafka |

No subir contraseñas, tokens ni secrets al repositorio.

## API y tópicos relevantes

| Servicio | Ruta | Función |
|---|---|---|
| 8080 | `/api/libros`, `/api/autores`, `/api/usuarios`, `/api/prestamos` | CRUD principal |
| 8080 | `/api/notificaciones`, `/api/auditoria` | Notificaciones e historial filtrable |
| 8081 | `/api/kafka/topics`, `/api/kafka/consumer-groups` | Administración Kafka |
| 8081 | `/api/kafka/publish-evento`, `/api/kafka/publish-invalid` | Publicación de prueba y fallo controlado |
| 8081 | `/api/kafka/metrics` | Métricas con polling |
| 8082 | `/api/rabbit/**` | Colas, exchanges, bindings y publicación |

Todas exigen `Authorization: Bearer <token>` de Entra, salvo `/api/public/**` y el perfil `dev`.

| Tópico | Uso | Particiones / réplicas | Grupo |
|---|---|---:|---|
| `biblioteca.notificaciones` | Avisos al usuario | 3 / 3 | `biblioteca-notificaciones` |
| `biblioteca.prestamos` | `PRESTAMO_CREADO` y `PRESTAMO_DEVUELTO` | 3 / 3 | `biblioteca-prestamos` |
| `*.DLT` | Mensajes fallidos | 3 / 3 | `biblioteca-dlt` |

Retención: 7 días en tópicos de negocio y 24 horas en DLT.

## Pruebas

```bash
./mvnw clean verify
```

Ejecuta 54 pruebas con H2 y un broker Kafka embebido; no necesita Docker ni base de datos externa.
