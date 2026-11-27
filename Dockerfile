# Dockerfile único para los tres microservicios ejecutables.
# El módulo se elige con el argumento SERVICE (ver docker-compose.backend.yml):
#   docker build --build-arg SERVICE=biblioteca-api -t biblioteca-api .
#
# La compilación se hace desde la raíz porque el proyecto es multi-módulo:
# -am incluye biblioteca-common automáticamente.

ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-21
FROM ${MAVEN_IMAGE} AS build

WORKDIR /workspace

# Copiar primero los POM aprovecha la caché de dependencias (la imagen
# Maven ya trae mvn, así que no hace falta descargar el wrapper).
COPY pom.xml ./
COPY biblioteca-common/pom.xml biblioteca-common/
COPY biblioteca-api/pom.xml biblioteca-api/
COPY biblioteca-admin-kafka/pom.xml biblioteca-admin-kafka/
COPY biblioteca-admin-rabbit/pom.xml biblioteca-admin-rabbit/

# Ahora sí entra el código (target/ y .env quedan fuera por .dockerignore).
COPY . .

ARG SERVICE=biblioteca-api
RUN mvn -B -ntp -pl ${SERVICE} -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy

ARG SERVICE=biblioteca-api
COPY --from=build /workspace/${SERVICE}/target/${SERVICE}-0.0.1-SNAPSHOT.jar /app/app.jar

WORKDIR /app
EXPOSE 8080 8081 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
