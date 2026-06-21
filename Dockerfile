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
