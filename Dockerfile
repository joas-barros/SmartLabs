# =========================================================
# Stage 1: Build the entire multi-module Maven project
# =========================================================
FROM maven:3-eclipse-temurin-25-alpine AS build
WORKDIR /app

# Copy pom files to cache dependency resolution
COPY pom.xml .
COPY devices/pom.xml devices/
COPY edge-gateway/pom.xml edge-gateway/
COPY mqtt-client/pom.xml mqtt-client/
COPY backend/ms-digital-twins/pom.xml backend/ms-digital-twins/
COPY backend/ms-laboratorio/pom.xml backend/ms-laboratorio/

RUN mvn dependency:go-offline -B

# Copy source code and build all jars + dependencies
COPY . .
RUN mvn package dependency:copy-dependencies -DskipTests

# =========================================================
# Stage 2: Runtime image for devices (Lab1Mqtt, Lab2Coap)
# =========================================================
FROM eclipse-temurin:25-jre-alpine AS devices
WORKDIR /app
COPY --from=build /app/devices/target/devices-1.0-SNAPSHOT.jar ./devices.jar
COPY --from=build /app/devices/target/dependency ./dependency
ENTRYPOINT ["java", "-cp", "devices.jar:dependency/*"]

# =========================================================
# Stage 3: Runtime image for edge-gateway
# =========================================================
FROM eclipse-temurin:25-jre-alpine AS edge-gateway
WORKDIR /app
COPY --from=build /app/edge-gateway/target/edge-gateway-1.0-SNAPSHOT.jar ./edge-gateway.jar
COPY --from=build /app/devices/target/devices-1.0-SNAPSHOT.jar ./devices.jar
COPY --from=build /app/edge-gateway/target/dependency ./dependency
ENTRYPOINT ["java", "-cp", "edge-gateway.jar:devices.jar:dependency/*", "br.ufersa.iot.gateway.EdgeGateway"]

# =========================================================
# Stage 4: Runtime image for mqtt-client
# =========================================================
FROM eclipse-temurin:25-jre-alpine AS mqtt-client
WORKDIR /app
COPY --from=build /app/mqtt-client/target/mqtt-client-1.0-SNAPSHOT.jar ./mqtt-client.jar
COPY --from=build /app/mqtt-client/target/dependency ./dependency
ENTRYPOINT ["java", "-cp", "mqtt-client.jar:dependency/*", "br.ufersa.iot.MqttClient"]

# =========================================================
# Stage 5: Runtime image for ms-digital-twins (Spring Boot)
# =========================================================
FROM eclipse-temurin:25-jre-alpine AS ms-digital-twins
WORKDIR /app
# O Spring Boot gera um "Fat JAR" com as dependências embutidas
COPY --from=build /app/backend/ms-digital-twins/target/ms-digital-twins-0.0.1-SNAPSHOT.jar ./ms-digital-twins.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "ms-digital-twins.jar"]

# =========================================================
# Stage 6: Runtime image for ms-laboratorio (Spring Boot)
# =========================================================
FROM eclipse-temurin:25-jre-alpine AS ms-laboratorio
WORKDIR /app
COPY --from=build /app/backend/ms-laboratorio/target/ms-laboratorio-0.0.1-SNAPSHOT.jar ./ms-laboratorio.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "ms-laboratorio.jar"]

# =========================================================
# Stage 7: Runtime image for api-gateway (Spring Boot)
# =========================================================
FROM eclipse-temurin:25-jre-alpine AS api-gateway
WORKDIR /app
COPY --from=build /app/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar ./api-gateway.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "api-gateway.jar"]
