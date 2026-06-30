# 🔬 SmartLabs - Academic IoT Monitoring Platform

![Java](https://img.shields.io/badge/Java-25-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-brightgreen.svg)
![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-AMQP-FF6600.svg)
![MQTT & CoAP](https://img.shields.io/badge/IoT-MQTT_%7C_CoAP-lightgrey.svg)

**SmartLabs** is a comprehensive distributed system focused on the Internet of Things (IoT) and Edge Computing. It simulates, collects, processes, and analyzes real-time data from academic laboratories (Computers, Air Conditioners, and Projectors), demonstrating resilience, scalability, and intelligent pattern detection.

## 🏗️ System Architecture

The architecture was designed following microservices and Edge Computing principles, divided into the following layers:

1. **Device Layer (Simulators):** Devices generate telemetry (CPU, RAM, Temperature) and events based on a dynamic script (Normal Use, Peak, Failures, Intrusions).
   - **Lab 1:** Communicates via the **MQTT** protocol (Publish/Subscribe) using Eclipse Mosquitto.
   - **Lab 2:** Communicates via the **CoAP** protocol (Req/Res optimized for IoT) using Eclipse Californium.
2. **Edge Layer (Edge Gateway):** Receives raw data, filters anomalies, executes business rules at the edge (e.g., immediate overheating alerts), aggregates metrics to save bandwidth, and acts as an offline buffer if the cloud connection drops.
3. **Messaging Layer (Cloud):** Uses **RabbitMQ** (AMQP) as the central *Message Broker*. The Edge Gateway publishes events to a *Topic Exchange*, ensuring total decoupling.
4. **Backend Layer (Microservices):**
   - **MS Digital Twins:** A reactive service (Spring WebFlux) focused on ultra-high performance. It consumes the RabbitMQ queue and maintains the real-time state (Snapshot of Now) in RAM.
   - **MS Laboratório:** A persistence and intelligence service. It saves the history in an H2 database and uses Sliding Time Windows to detect complex patterns (lab overload, heating trends, correlations).
5. **API Gateway:** The single entry point of the architecture. It routes requests and implements the **BFF (Backend for Frontend)** pattern, consolidating data from multiple microservices simultaneously via `Mono.zip`.

---

## 📂 Repository Structure (Modules)

The project is built as a *Multi-module Maven Project*:

* `devices/`: Autonomous IoT Simulators (Threads, Jitter, Dynamic Scripting).
* `edge-gateway/`: Edge IoT Gateway (Filters, Periodic aggregations, Offline Buffer).
* `backend/ms-digital-twins/`: Real-time state microservice.
* `backend/ms-laboratorio/`: Temporal analysis and historical data microservice.
* `api-gateway/`: Unified router (Port 8080).
* `cliente-http/`: CLI application for interactive REST API consumption.
* `mqtt-client/`: Interactive CLI application to audit and debug pure MQTT messages.
* `mosquitto/`: Local MQTT Broker configurations.

---

## 🚀 How to Run (Docker Compose)

The entire system infrastructure has been "containerized". To run the whole system with a single command, you just need **Docker** and **Docker Compose** installed.

1. Clone the repository and navigate to the project root.
2. Build and spin up all services:
   ```bash
   docker-compose up --build -d
   

3. Check if all containers are running:
```bash
docker-compose ps

```



*Note: Docker will automatically start Mosquitto, RabbitMQ, the Edge Gateway, the Simulators for both Labs, the Microservices, and the API Gateway.*

---

## 🕹️ Testing the CLI Tools (Clients)

Since the system is backend-focused, we created excellent command-line interfaces (CLI) to interact with the architecture.

### 1. HTTP Client (Main Dashboard)

This client consumes the API Gateway endpoints and lets you see the magic of data aggregation.
Open a terminal (requires Java installed) and run the main class via your IDE or Maven:

```bash
cd "cliente-http (1)/cliente-http"
mvn spring-boot:run

```

*Try option `8` (Consolidated Dashboard) to see cross-referenced performance and historical data in real-time!*

### 2. MQTT Auditor

Want to see the "raw" data traveling through the network before it reaches the backend?

```bash
cd mqtt-client
mvn exec:java -Dexec.mainClass="br.ufersa.iot.MqttClient"

```

*In the prompt, type `sub lab/#` to listen to all Lab 1 devices simultaneously.*

---

## 📊 Internal Monitoring

### RabbitMQ Dashboard

You can visualize queues filling up, consumers consuming, and the message rate (Throughput) graph in real-time:

* **URL:** [http://localhost:15672](https://www.google.com/search?q=http://localhost:15672)
* **User:** `guest`
* **Password:** `guest`

*(Practical resilience test: Stop the `ms-laboratorio` container, watch the messages accumulate in RabbitMQ, and start it again to see the queue being consumed without losing any data!)*

---

## ✨ Highlighted Features

* **Network Resilience (Offline Mode):** The Edge Gateway detects connection drops with RabbitMQ, saves data locally in a queue (Offline Cache), and automatically resyncs when the internet connection is restored.
* **Auto-Healing (Retry):** Microservices have connection retry loops to tolerate slowness during infrastructure startup.
* **Sliding Windows:** The local AI in `ms-laboratorio` doesn't look at isolated readings; instead, it looks at blocks of the last 10 readings to confirm "Persistent High CPU".
* **Thread-Safety:** Deep use of concurrent safe collections (`ConcurrentHashMap`, `AtomicReference`, `LinkedBlockingQueue`) to support thousands of events per second in a WebFlux environment.
