# Backend Biblioteca

API backend para el proyecto académico **Cloud Native I**. La aplicación está construida con Spring Boot y expone servicios REST para la gestión de una biblioteca.

## Estado del proyecto

- Estado: implementación local funcional.
- Evaluación: Evaluación 1.
- Persistencia: H2 en memoria.
- Seguridad: Spring Security como OAuth2 Resource Server con JWT.
- Proveedor de identidad: Microsoft Entra ID.
- Frontend: pendiente de integración con MSAL.
- API Gateway: pendiente de configuración en AWS.

> La integración completa con frontend y API Gateway está planificada, pero todavía no debe considerarse implementada.

## Arquitectura actual

```text
Cliente HTTP / futuro frontend
              |
              v
      Spring Boot API
              |
      Spring Security
              |
      Microsoft Entra ID
              |
              v
        Base de datos H2
```

Flujo objetivo:

```text
Frontend con MSAL
        |
        | Access Token JWT
        v
Backend Biblioteca
        |
        | validación de firma, issuer, audiencia y expiración
        v
Controladores REST
```

La aplicación se comporta como un **OAuth2 Resource Server**: recibe Access Tokens JWT emitidos por Microsoft Entra ID y valida que estén destinados a esta API antes de permitir el acceso a las rutas protegidas.

## Tecnologías

- Java.
- Spring Boot.
- Spring Web.
- Spring Data JPA.
- Spring Security.
- OAuth2 Resource Server.
- JWT.
- Microsoft Entra ID.
- H2 Database.
- Maven Wrapper.

## Requisitos previos

Instalar:

- Java compatible con la versión declarada en `pom.xml`.
- Git.
- PowerShell en Windows o una terminal equivalente.

No es necesario instalar MySQL ni PostgreSQL para ejecutar la versión actual, porque la aplicación utiliza H2 en memoria.

## Estructura relevante

```text
src/
└── main/
    ├── java/com/biblioteca/
    │   ├── config/
    │   │   └── SecurityConfig.java
    │   └── controller/
    │       └── HealthController.java
    └── resources/
        └── application.properties
```

La estructura exacta puede contener además entidades, repositorios, servicios y controladores de dominio de la biblioteca.

## Configuración de Microsoft Entra ID

La API está registrada en Microsoft Entra ID como:

```text
Backend-Biblioteca
```

La aplicación expone estos permisos delegados:

```text
api://a328191c-fd63-40fc-8994-b9260a828929/recurso.read
api://a328191c-fd63-40fc-8994-b9260a828929/recurso.write
```

Datos utilizados por el backend:

```text
Backend client ID:
a328191c-fd63-40fc-8994-b9260a828929

Tenant ID:
a94e2d64-6e37-4765-8ceb-bfe6bc09932b
```

Estos identificadores representan la API y el tenant. No son client secrets ni tokens. Nunca deben agregarse al proyecto contraseñas, tokens, claves privadas o client secrets.

## Configuración local

Archivo:

```text
src/main/resources/application.properties
```

Configuración principal actual:

```properties
spring.application.name=biblioteca-backend

spring.datasource.url=jdbc:h2:mem:biblioteca_db;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false

server.port=8080

logging.level.com.biblioteca=DEBUG

app.security.issuer-uri=https://login.microsoftonline.com/a94e2d64-6e37-4765-8ceb-bfe6bc09932b/v2.0
app.security.audience=a328191c-fd63-40fc-8994-b9260a828929
app.security.jwk-set-uri=https://login.microsoftonline.com/a94e2d64-6e37-4765-8ceb-bfe6bc09932b/discovery/v2.0/keys
```

En un entorno productivo, estos valores deben externalizarse mediante variables de entorno, un gestor de secretos o la configuración segura del entorno de despliegue.

## Seguridad

La configuración de `SecurityConfig` aplica las siguientes reglas:

- La aplicación no utiliza sesiones HTTP; trabaja de forma stateless.
- Las rutas `OPTIONS` están permitidas para facilitar CORS.
- `/h2-console/**` está permitido para desarrollo local.
- `/api/public/**` está permitido sin token.
- `/error` está permitido.
- `/api/**` requiere autenticación.
- El resto de las rutas queda permitido por la configuración actual.
- La consola H2 usa `sameOrigin` para permitir su funcionamiento local.
- La autenticación utiliza Bearer Tokens JWT.
- Se valida la firma del token usando el JWKS de Microsoft Entra ID.
- Se valida el issuer.
- Se valida la audiencia de la API.
- Se valida la expiración mediante los validadores estándar de Spring Security.

### Respuestas esperadas

```text
200 OK
La solicitud fue procesada correctamente.

401 Unauthorized
No existe un token válido, está ausente, expiró o no pudo validarse.

403 Forbidden
El token es válido, pero el usuario o aplicación no tiene permisos suficientes.
```

## CORS

Actualmente se permite el origen local de Angular:

```text
http://localhost:4200
```

Métodos permitidos:

```text
GET, POST, PUT, PATCH, DELETE, OPTIONS
```

Headers permitidos:

```text
Authorization, Content-Type, Accept
```

Cuando se integre otro frontend, por ejemplo React en `http://localhost:5173`, se deberá agregar explícitamente ese origen en `SecurityConfig`. No se recomienda usar `*` en producción.

## Ejecución local

### 1. Clonar el repositorio

```powershell
git clone <URL_DEL_REPOSITORIO>
cd backend-biblioteca
```

### 2. Ejecutar pruebas

En Windows:

```powershell
.\mvnw.cmd clean test
```

En Linux o macOS:

```bash
./mvnw clean test
```

Resultado esperado:

```text
BUILD SUCCESS
```

### 3. Iniciar el backend

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

En Linux o macOS:

```bash
./mvnw spring-boot:run
```

La API quedará disponible en:

```text
http://localhost:8080
```

### 4. Detener el backend

En la terminal donde está ejecutándose:

```text
Ctrl + C
```

## Pruebas manuales

### Endpoint público

PowerShell:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/public/health
```

Resultado esperado:

```text
StatusCode : 200
Content    : {"status":"UP","service":"biblioteca-backend"}
```

### Endpoint protegido sin token

PowerShell:

```powershell
try {
    Invoke-WebRequest -UseBasicParsing http://localhost:8080/api/libros
} catch {
    $_.Exception.Response.StatusCode.value__
}
```

Resultado esperado:

```text
401
```

Este resultado confirma que `/api/libros` está protegido y que una solicitud sin Access Token no es aceptada.

### Endpoint protegido con token

Todavía queda pendiente obtener un Access Token real desde el frontend mediante MSAL. Cuando el frontend esté configurado, la llamada tendrá esta forma:

```powershell
$token = "ACCESS_TOKEN_REAL"

Invoke-WebRequest `
  -UseBasicParsing `
  -Headers @{ Authorization = "Bearer $token" } `
  http://localhost:8080/api/libros
```

El resultado esperado será `200` si el token tiene una audiencia, issuer, vigencia y permisos válidos.

No guardar tokens reales en este README ni en el repositorio.

## Consola H2

La consola H2 está disponible localmente en:

```text
http://localhost:8080/h2-console
```

Usar estos datos:

```text
JDBC URL:
jdbc:h2:mem:biblioteca_db;DB_CLOSE_DELAY=-1

User Name:
sa

Password:
Dejar vacío
```

La base de datos es en memoria. Todos los datos se pierden cuando se detiene o reinicia el backend.

La consola H2 está habilitada solo para desarrollo y no debe exponerse públicamente en producción.

## Problema frecuente: puerto 8080 ocupado

Si aparece:

```text
Web server failed to start. Port 8080 was already in use.
```

significa que otra instancia del backend ya está ejecutándose o que otro proceso está usando el puerto.

Buscar el proceso:

```powershell
netstat -ano | findstr :8080
```

Identificar el proceso:

```powershell
tasklist /FI "PID eq NUMERO_PID"
```

Si corresponde al backend anterior, detenerlo:

```powershell
taskkill /PID NUMERO_PID /F
```

También puede bastar con volver a la terminal donde el backend está corriendo y presionar `Ctrl + C`. No se deben ejecutar dos instancias en el mismo puerto.

## Estado de implementación

### Implementado

- Proyecto Spring Boot ejecutable.
- API REST local.
- Persistencia H2 en memoria.
- Configuración JPA.
- Endpoint público de salud.
- Configuración CORS.
- Spring Security.
- OAuth2 Resource Server.
- Validación JWT con JWKS de Microsoft Entra ID.
- Validación de issuer.
- Validación de audience.
- Protección de rutas `/api/**`.
- Pruebas Maven exitosas.
- Pruebas manuales `200` para endpoint público y `401` para endpoint protegido sin token.

### Pendiente

- Registrar la aplicación SPA del frontend en Microsoft Entra ID.
- Configurar MSAL en el frontend.
- Solicitar scopes `recurso.read` y `recurso.write` desde el frontend.
- Probar una llamada autenticada real con Access Token.
- Aplicar autorización específica por scope en métodos mediante `@PreAuthorize`.
- Configurar un API Gateway o API Manager.
- Publicar el backend en un entorno cloud.
- Externalizar la configuración sensible por ambiente.
- Agregar observabilidad, métricas y trazabilidad.
- Agregar pruebas automatizadas de seguridad para `200`, `401` y `403`.

## Evolución futura

Arquitectura objetivo:

```text
Frontend Angular o React
          |
          | MSAL / Authorization Code + PKCE
          v
Microsoft Entra ID
          |
          | Access Token JWT
          v
AWS API Gateway o Azure API Management
          |
          v
Backend Spring Boot
          |
          v
Base de datos o servicio interno
```

En la evolución del proyecto se recomienda:

- Mantener las rutas versionadas, por ejemplo `/api/v1/libros`.
- Validar scopes específicos por operación.
- Restringir CORS a los dominios reales.
- Usar HTTPS.
- Mover secretos y configuración a un gestor seguro.
- Deshabilitar la consola H2 en producción.
- Utilizar migraciones como Flyway o Liquibase.
- Configurar rate limiting y monitoreo en el gateway.
- No registrar contraseñas ni tokens completos en los logs.

## Relación con la Evaluación 1

El backend implementa conceptos de la Evaluación 1:

- API REST.
- CORS.
- OAuth 2.0.
- OpenID Connect como base de identidad.
- Microsoft Entra ID como IDaaS.
- JWT.
- Resource Server.
- Protección de endpoints.
- Separación entre autenticación y autorización.
- Integración futura con frontend y API Gateway.

## Checklist de validación

- [x] `./mvnw clean test` termina con `BUILD SUCCESS`.
- [x] El backend inicia en `localhost:8080`.
- [x] `/api/public/health` responde `200`.
- [x] `/api/libros` sin token responde `401`.
- [x] CORS permite el frontend local configurado.
- [x] Microsoft Entra ID está configurado como proveedor de identidad.
- [x] La API expone `recurso.read` y `recurso.write`.
- [ ] El frontend obtiene un Access Token real.
- [ ] Una ruta protegida responde `200` con token válido.
- [ ] Se demuestra un caso `403` por permisos insuficientes.
- [ ] API Gateway está configurado.
- [ ] El backend está desplegado en la nube.

## Seguridad y buenas prácticas

- No incluir client secrets en el frontend.
- No subir tokens JWT al repositorio.
- No subir contraseñas reales.
- No guardar claves privadas en Git.
- Usar variables de entorno o un gestor de secretos para despliegues.
- Verificar issuer, audience, firma y expiración de todos los JWT.
- Usar `401` para solicitudes no autenticadas y `403` para solicitudes sin autorización suficiente.
- No dejar H2 Console habilitada en producción.
- No permitir CORS abierto sin una razón documentada.

## Licencia y uso académico

Proyecto desarrollado con fines académicos para la asignatura Cloud Native I. Ajustar la licencia y los datos del equipo según las instrucciones del repositorio principal.