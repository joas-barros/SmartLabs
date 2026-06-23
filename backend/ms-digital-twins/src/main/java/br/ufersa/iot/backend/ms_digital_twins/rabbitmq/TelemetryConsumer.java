package br.ufersa.iot.backend.ms_digital_twins.rabbitmq;

import br.ufersa.iot.backend.ms_digital_twins.config.RabbitMqConfig;
import br.ufersa.iot.backend.ms_digital_twins.service.DigitalTwinService;
import com.rabbitmq.client.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;


import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class TelemetryConsumer {
    private final DigitalTwinService service;
    private final ObjectMapper objectMapper;

    private Connection connection;
    private Channel channel;

    public TelemetryConsumer(DigitalTwinService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void startListening() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitMqConfig.HOST);
        factory.setPort(RabbitMqConfig.PORT);
        factory.setUsername(RabbitMqConfig.USER);
        factory.setPassword(RabbitMqConfig.PASS);
        factory.setAutomaticRecoveryEnabled(true);

        int tentativas = 0;
        int maxTentativas = 10;

        while (tentativas < maxTentativas && (this.connection == null || !this.connection.isOpen())) {
            try {
                this.connection = factory.newConnection();
                this.channel = connection.createChannel();

                // Declara a Exchange e a Fila
                channel.exchangeDeclare(RabbitMqConfig.EXCHANGE_EVENTOS, BuiltinExchangeType.TOPIC, true);
                channel.queueDeclare(RabbitMqConfig.FILA_DIGITAL_TWINS, true, false, false, null);

                // Liga a fila à exchange usando o wildcard '#' (recebe TUDO)
                channel.queueBind(RabbitMqConfig.FILA_DIGITAL_TWINS, RabbitMqConfig.EXCHANGE_EVENTOS, RabbitMqConfig.ROUTING_KEY_WILDCARD);

                System.out.println("[AMQP-RX] Conectado ao RabbitMQ nativo! Escutando a fila: " + RabbitMqConfig.FILA_DIGITAL_TWINS);

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String routingKey = delivery.getEnvelope().getRoutingKey();
                    String payload = new String(delivery.getBody(), StandardCharsets.UTF_8);

                    // Encaminha a mensagem consoante a routing key que o Gateway utilizou
                    if (routingKey.startsWith("telemetria")) {
                        processTelemetry(payload);
                    } else if (routingKey.startsWith("alerta")) {
                        processAlert(payload);
                    }
                };

                // Inicia o consumo das mensagens (autoAck = true para este caso de uso em tempo real)
                channel.basicConsume(RabbitMqConfig.FILA_DIGITAL_TWINS, true, deliverCallback, consumerTag -> { });

                break; // Conectou com sucesso, sai do loop de tentativas

            } catch (Exception e) {
                tentativas++;
                System.err.printf("[AMQP-RX] Falha ao conectar no RabbitMQ (%d/%d): %s. Tentando novamente em 5s...%n",
                        tentativas, maxTentativas, e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void processTelemetry(String payload) {
        try {
            Map<String, Object> data = objectMapper.readValue(payload, new TypeReference<>() {});
            String lab = String.valueOf(data.get("lab"));
            String id = String.valueOf(data.get("id"));

            String type = "DESCONHECIDO";
            if (data.containsKey("cpu")) type = "PC";
            else if (data.containsKey("environmentTemperature")) type = "AC";
            else if (data.containsKey("internalTemperature")) type = "PROJECTOR";

            if (lab != null && id != null) {
                // Subscreve a operação no repositório reativo
                service.processTelemetry(lab, id, type, data).subscribe();
            }
        } catch (Exception e) {
            System.err.println("[AMQP-RX] Erro ao processar telemetria: " + e.getMessage());
        }
    }

    private void processAlert(String payload) {
        try {
            Map<String, Object> data = objectMapper.readValue(payload, new TypeReference<>() {});
            String lab = String.valueOf(data.get("lab"));
            String id = data.containsKey("dispositivo") ? String.valueOf(data.get("dispositivo")) : String.valueOf(data.get("pc"));
            String alerta = String.valueOf(data.get("alerta"));

            if (lab != null && id != null && alerta != null) {
                service.processAlert(lab, id, "⚠️ ALERTA: " + alerta).subscribe();
                System.out.println("[TWIN-UPDATED] Evento de alerta adicionado: " + lab + "/" + id);
            }
        } catch (Exception e) {
            System.err.println("[AMQP-RX] Erro ao processar alerta: " + e.getMessage());
        }
    }

    @PreDestroy
    public void close() {
        System.out.println("[AMQP-RX] Encerrando conexão nativa com RabbitMQ...");
        try {
            if (channel != null && channel.isOpen()) channel.close();
            if (connection != null && connection.isOpen()) connection.close();
        } catch (Exception e) {
            System.err.println("Erro ao fechar conexão RabbitMQ: " + e.getMessage());
        }
    }
}
