# Guía de evidencias EP5 — DSY1107 (con referencias y rúbrica)

Cada evidencia lleva:

- **Ref.:** `EV-01`, `EV-02`, … (identificador único para el informe).
- **📷 Sacar foto:** momento exacto de la captura y nombre sugerido del archivo.
- **Rúbrica:** indicador `R1`–`R9` de la pauta (o `RG` = requisito general de las instrucciones específicas).

Guarda todo en: `C:\Users\frang\Documents\Duoc\cloud\Ev3\Prueba 3\Evidencias`.

> Los avisos (snackbars) duran 3–4 s: **sacar foto en cuanto aparecen**. Si se pierde, repite la acción.
> No pegues el token JWT completo en el informe; con la foto del header basta.

---

## Rúbrica usada como referencia (Pauta de Evaluación, 9 indicadores)

| Código | Indicador (resumen) | Peso |
|---|---|---:|
| **R1** | Configuración completa del cliente Kafka: brokers en Docker Compose, serializers, consumer groups y propiedades avanzadas; publica y consume en los dos tópicos con particiones/réplicas definidas. | 12% |
| **R2** | Productores y consumidores interactúan con los dos tópicos usando DTO tipados, de forma coherente con la arquitectura. | 12% |
| **R3** | Microservicio administrador de Kafka: crear, listar y mantener tópicos y configuraciones básicas. | 6% |
| **R4** | Productores Kafka con DTO bien estructurados y serializers adecuados. | 14% |
| **R5** | Consumidores Kafka (`@KafkaListener` o equivalente) con lógica de negocio separada de la mensajería. | 14% |
| **R6** | Manejo de errores: validación de payload, logging contextual y control de reintentos (DLT). | 12% |
| **R7** | Parámetros de administración de tópicos: retención, cleanup policy, particiones y factor de réplicas. | 12% |
| **R8** | Consumer groups para balanceo de carga y procesamiento resiliente. | 12% |
| **R9** | Operaciones controladas y replicables para administrar tópicos y consumer groups (microservicio/scripts). | 6% |
| **RG** | Requisitos generales de las instrucciones específicas: JWT del IDaaS, login en el front, RabbitMQ sin afectar, repositorios GitHub/.gitignore. No tiene peso propio en la pauta. | — |

---

## 0. Preparación (antes de cualquier foto)

En la misma terminal donde usarás Docker:

```powershell
Remove-Item Env:\SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
docker compose -f docker-compose.backend.yml up -d --force-recreate --no-build
```

Espera 30–60 s y comprueba:

```powershell
curl.exe http://localhost:8080/api/public/health
curl.exe -i http://localhost:8080/api/auditoria
```

- Health → `200`.
- Auditoría sin token → `401` (JWT activo). Si da `200`, queda perfil `dev`: repite el paso.

Arranca el front:

```powershell
cd "C:\Users\frang\Documents\Duoc\cloud\Ev3\Prueba 3\Front y back\front_proyecto_cloud-main"
npm run start:local
```

Abre `http://localhost:4200` e inicia sesión con Entra.

| Ref. | Qué poner / hacer | 📷 Sacar foto | Rúbrica |
|---|---|---|---|
| **EV-00** | Terminal con el `401` sin token + barra del front con tu correo | Al confirmar que JWT está activo; `00-jwt-activo.png` | **RG** (filtro JWT / login IDaaS) |

---

## 1. Infraestructura y topología

| Ref. | Qué poner / hacer | 📷 Sacar foto | Rúbrica |
|---|---|---|---|
| **EV-01** | `docker ps --format "table {{.Names}}\t{{.Status}}"` | Al terminar el paso 0; deben verse 3 ZK, 3 brokers, `kafka-ui`, `rabbit1/2` y los 3 microservicios `Up`. Archivo `01-infra.png` | **R1**, **R9** |
| **EV-02** | Kafka UI (`http://localhost:8085`) → pestaña **Brokers** | Con los 3 brokers online; `02-brokers.png` | **R1** |
| **EV-03** | Kafka UI → **Topics** | Con los 4 tópicos visibles (3 particiones / 3 réplicas); `03-topics.png` | **R7**, **R3** |
| **EV-04** | Abrir `biblioteca.notificaciones` → particiones | Con particiones 0,1,2 e **ISR = 3**; `04-isr.png` | **R1**, **R7** |
| **EV-05** | Pestaña **Config** de `biblioteca.notificaciones` | Con `retention.ms=604800000`, `cleanup.policy=delete`, `min.insync.replicas=2`; `05-config.png` | **R7** |
| **EV-06** | Config de `biblioteca.notificaciones.DLT` | Con `retention.ms=86400000` (24 h); `06-dlt-config.png` | **R7**, **R6** |
| **EV-07** | Kafka UI → **Consumer Groups** | Con `biblioteca-notificaciones`, `biblioteca-prestamos`, `biblioteca-dlt` en `STABLE`; `07-groups.png` | **R8** |

---

## 2. Front, JWT y CORS

| Ref. | Qué poner / hacer | 📷 Sacar foto | Rúbrica |
|---|---|---|---|
| **EV-08** | En Angular, entrar a **Streaming (Kafka)** | Con la tabla de tópicos y el botón **Abrir Kafka UI** visibles; `08-front-kafka.png` | **R3**, **RG** |
| **EV-09** | F12 → **Network** → recargar → clic en `GET http://localhost:8081/api/kafka/topics` → **Headers** | Con `Authorization: Bearer eyJ...`, `Origin: http://localhost:4200` y status `200`; `09-jwt-cors.png` | **RG** (JWT en llamadas), **R3** |

---

## 3. CRUD de tópicos (usar solo `biblioteca.evidencia`)

No borres los tópicos de la aplicación.

| Ref. | Qué poner / hacer | 📷 Sacar foto | Rúbrica |
|---|---|---|---|
| **EV-10** | **Crear tópico**: Nombre `biblioteca.evidencia`, Particiones `3`, Réplicas `3`, retention.ms `604800000` → **Crear** | Al aparecer «Tópico creado» y la fila nueva; `10-create.png` | **R3**, **R9** |
| **EV-11** | Icono 🔧 **Ver configuración** de ese tópico | Con el detalle abierto (`retention.ms`, `cleanup.policy`); `11-get.png` | **R3**, **R7** |
| **EV-12** | En el detalle: particiones `6`, retention.ms `86400000`, cleanup.policy `delete` → **Guardar cambios** | Con «Tópico actualizado» y 6 particiones; `12-put.png` | **R3**, **R7**, **R9** |
| **EV-13** | Icono 🗑 **Eliminar** → botón **Eliminar** del diálogo | Con el diálogo de confirmación abierto; `13-delete-confirm.png` | **R3**, **R9** |
| **EV-14** | — | Justo tras confirmar: «Tópico eliminado» y la fila desaparece; `14-delete.png` | **R3**, **R9** |

> Bajar particiones devuelve `400`; đó NO es evidencia de éxito. Solo aumenta de 3 a 6.

---

## 4. Publicación del DTO de 7 campos

Pestaña **Publicar evento**. Qué poner:

- Tópico: `biblioteca.prestamos`
- id (key): `evidencia-prestamo-1`
- tipo: `PRESTAMO_CREADO`
- destinatario: `ana@correo.cl`
- contenido: `Evidencia préstamo EP5`
- correlationId: `evidencia-corr-prestamo-1`
- `fechaEnvio` y `usuario`: **no se escriben**, se generan solos.

| Ref. | 📷 Sacar foto | Rúbrica |
|---|---|---|
| **EV-15** | Formulario completo antes de enviar; `15-dto-form.png` | **R4**, **R2** |
| **EV-16** | Nada más pulsar **Publicar evento**: aviso con id y estado `PENDIENTE`; `16-dto-202.png` | **R4**, **R2** |
| **EV-17** | Kafka UI → `biblioteca.prestamos` → mensaje con key `evidencia-prestamo-1` y value con los 7 campos, sin `@type`; `17-kafka-ui-dto.png` | **R4**, **R2** |

---

## 5. Publicación rápida

| Ref. | Qué poner / hacer | 📷 Sacar foto | Rúbrica |
|---|---|---|---|
| **EV-18** | Volver a seleccionar Tópico `biblioteca.notificaciones` en el primer formulario (la publicación rápida usa ese tópico). En **Publicación rápida** → Contenido `Hola desde el front evidencia` → **Publicar simple** | Con el aviso `202` y el id generado; `18-publish-simple.png` | **R4** |

---

## 6. Consumo y métricas

| Ref. | Qué poner / hacer | 📷 Sacar foto | Rúbrica |
|---|---|---|---|
| **EV-19** | Pestaña **Métricas en vivo**; esperar **5–10 s** (un polling) | Con `Eventos/min > 0`, `Lag total = 0`, grupos `STABLE` y distribución con lag 0; `19-metrics-normal.png` | **R5**, **R8** |

> La primera muestra puede ser `0.0`. Espera al menos un intervalo antes de sacar la foto.

---

## 7. Error controlado y DLT

| Ref. | Qué poner / hacer | 📷 Sacar foto | Rúbrica |
|---|---|---|---|
| **EV-20** | En **Publicar evento**, botón **Simular fallo** | Nada más aparecer el aviso `202` con id; `20-dlt-publish.png` | **R6** |
| **EV-21** | **Esperar 10–15 s** (4 entregas: inicial + 3 reintentos, backoff 1 s). Kafka UI → `biblioteca.notificaciones.DLT` | Con el mensaje de contenido vacío en el DLT; `21-dlt-message.png` | **R6** |
| **EV-22** | Volver a **Métricas en vivo** | Con `Fallos totales > 0`, `Fallos/min > 0` y `Lag total = 0`; `22-metrics-failures.png` | **R6** |
| **EV-23** | `docker logs biblioteca-api --tail 60` (opcional, refuerza logging) | Con el error del consumidor y el motivo del rechazo; `23-logs-dlt.png` | **R6** |

---

## 8. Auditoría real con usuario Entra

| Ref. | Qué poner / hacer | 📷 Sacar foto | Rúbrica |
|---|---|---|---|
| **EV-24** | En **Publicación de negocio (con auditoría)**: destinatario `ana@correo.cl`, contenido `Demo auditoría evidencia` → **Publicar notificación** | Con el texto «Última publicación: HTTP 202 … estado PENDIENTE»; `24-audit-publish.png` | **R4**, **RG** |
| **EV-25** | Esperar **3–5 s** → pestaña **Auditoría** → **Filtrar** sin filtros | Con la fila: `usuario` = tu correo, `correlationId` no nulo, `canal=biblioteca.notificaciones`, `estado=PROCESADO`, `fechaProcesamiento` y `mensajeId`; `25-audit-list.png` | **R2**, **R5**, **RG** |
| **EV-26** | Clic en copiar del `correlationId` → pegarlo en **Correlation ID** → **Filtrar** | Con una sola fila filtrada; `26-audit-filter.png` | **R2**, **R5** |

---

## 9. Comandos de respaldo y regresión

| Ref. | Qué poner | 📷 Sacar foto | Rúbrica |
|---|---|---|---|
| **EV-27** | `docker exec kafka1 kafka-consumer-groups --bootstrap-server localhost:29092 --describe --group biblioteca-notificaciones` | Con `STATE STABLE` y `LAG 0`; `27-cli-group.png` | **R8** |
| **EV-28** | `docker exec kafka1 kafka-topics --bootstrap-server localhost:29092 --describe --topic biblioteca.notificaciones` | Con particiones 0,1,2, `Replicas: 1,2,3`, `Isr: 1,2,3`; `28-cli-topic.png` | **R1**, **R7** |
| **EV-29** | Abrir **Mensajería (RabbitMQ)** | Con colas/exchanges/bindings; `29-rabbit-ok.png` | **RG** (Rabbit sin afectar) |
| **EV-30** | Abrir **Notificaciones** | Con la tabla cargada; `30-notificaciones-ok.png` | **RG** (Rabbit sin afectar) |

---

## 10. Fotos del código (recomendadas: la pauta evalúa código)

| Ref. | Qué poner | 📷 Sacar foto | Rúbrica |
|---|---|---|---|
| **EV-31** | Abrir `backend-biblioteca/biblioteca-common/src/main/resources/application-common.yml` | Con las líneas del cliente Kafka (bootstrap, serializers, `min.insync.replicas`, reintentos, consumer groups, `polling-interval-seconds`); `31-config-cliente.png` | **R1**, **R7**, **R8** |
| **EV-32** | Abrir `backend-biblioteca/biblioteca-api/src/main/java/com/biblioteca/messaging/kafka/KafkaEventoPublisher.java` y `backend-biblioteca/biblioteca-admin-kafka/src/main/java/com/biblioteca/messaging/kafka/KafkaAdminPublisher.java` | Con el productor y el DTO `EventoMensajeria`; `32-productores.png` | **R4**, **R2** |
| **EV-33** | Abrir `backend-biblioteca/biblioteca-api/src/main/java/com/biblioteca/messaging/kafka/KafkaEventoConsumer.java` y `backend-biblioteca/biblioteca-api/src/main/java/com/biblioteca/messaging/consumer/dlq/DeadLetterConsumer.java` | Con `@KafkaListener`, validación y envío al DLT; `33-consumidores-dlt.png` | **R5**, **R6** |
| **EV-34** | Abrir `backend-biblioteca/biblioteca-admin-kafka/src/main/java/com/biblioteca/controller/KafkaAdminController.java` | Con los endpoints GET/POST/PUT/DELETE; `34-admin-endpoints.png` | **R3**, **R9** |

---

## Matriz rápida: indicador de la rúbrica → evidencias

| Indicador | Peso | Evidencias que lo cubren |
|---|---:|---|
| **R1** Cliente Kafka completo | 12% | EV-01, EV-02, EV-04, EV-28, EV-31 |
| **R2** Productores/consumidores con 2 tópicos y DTO | 12% | EV-15, EV-16, EV-17, EV-25, EV-26, EV-32 |
| **R3** Administrador de tópicos | 6% | EV-03, EV-08, EV-09, EV-10–EV-14, EV-34 |
| **R4** Productores con DTO/serializers | 14% | EV-15–EV-18, EV-24, EV-32 |
| **R5** Consumidores `@KafkaListener` | 14% | EV-19, EV-25, EV-26, EV-33 |
| **R6** Errores, reintentos y DLT | 12% | EV-06, EV-20–EV-23, EV-33 |
| **R7** Parámetros de tópicos | 12% | EV-03–EV-06, EV-11, EV-12, EV-28, EV-31 |
| **R8** Consumer groups | 12% | EV-07, EV-19, EV-27, EV-31 |
| **R9** Administración replicable | 6% | EV-01, EV-10–EV-14, EV-34 |
| **RG** Requisitos generales | — | EV-00, EV-08, EV-09, EV-24, EV-25, EV-29, EV-30 |

**Total de la pauta cubierto: 100% (9 indicadores).**
