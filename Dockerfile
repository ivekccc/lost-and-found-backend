# syntax=docker/dockerfile:1.7
#
# Spring Boot 4.0.1 / Java 21 backend for Lost & Found.
#
# Build:
#   cd lost-and-found-backend
#   docker build -t lostandfound-backend:latest .
#
# Run locally (uses host Postgres):
#   docker run --rm -p 8082:8082 \
#     -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/lost-and-found \
#     -e DB_USERNAME=postgres -e DB_PASSWORD=admin \
#     -e MAIL_USERNAME=... -e MAIL_PASSWORD=... \
#     -e JWT_SECRET=... \
#     lostandfound-backend:latest
#
# Run against Neon Postgres (production-like):
#   docker run --rm -p 8082:8082 \
#     -e SPRING_DATASOURCE_URL='jdbc:postgresql://ep-xxx.neon.tech/lost-and-found?sslmode=require' \
#     -e DB_USERNAME=... -e DB_PASSWORD=... \
#     -e JWT_SECRET=... \
#     lostandfound-backend:latest

###############################################################################
# Stage 1: build
# Maven + JDK 21 → packaged Spring Boot JAR, then split into layers.
###############################################################################
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Maven wrapper + pom first for dep-resolution caching
COPY mvnw mvnw
COPY .mvn .mvn
COPY pom.xml pom.xml
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Source + package (skip tests; they need a Postgres test container)
COPY src src
RUN ./mvnw -B -q -DskipTests package \
 && mkdir -p /workspace/extracted \
 && cp target/*.jar /workspace/extracted/app.jar \
 && cd /workspace/extracted \
 && java -Djarmode=layertools -jar app.jar extract

###############################################################################
# Stage 2: runtime
# Slim JRE image. Layers copied separately so app code changes don't bust
# the (large, slow) dependencies layer.
###############################################################################
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Non-root user (Azure App Service / Container Apps best practice)
RUN groupadd --system --gid 1001 spring \
 && useradd  --system --uid 1001 --gid spring spring

# Spring Boot layered JAR — order matters (least → most changing)
COPY --from=build --chown=spring:spring /workspace/extracted/dependencies/         ./
COPY --from=build --chown=spring:spring /workspace/extracted/spring-boot-loader/   ./
COPY --from=build --chown=spring:spring /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /workspace/extracted/application/          ./

USER spring

# Azure App Service / Container Apps inject PORT; default to 8082 locally.
ENV SERVER_PORT=8082 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8082

# `exec` so Java is PID 1 and receives SIGTERM cleanly on container stop.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-${SERVER_PORT}} org.springframework.boot.loader.launch.JarLauncher"]
