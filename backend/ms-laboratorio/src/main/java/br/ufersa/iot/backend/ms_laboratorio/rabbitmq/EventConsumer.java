package br.ufersa.iot.backend.ms_laboratorio.rabbitmq;

import br.ufersa.iot.backend.ms_laboratorio.config.RabbitMqConfig;
import br.ufersa.iot.backend.ms_laboratorio.model.EventoGateway;
import br.ufersa.iot.backend.ms_laboratorio.service.LaboratorioService;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper; // CORRIGIDO O IMPORT
import com.fasterxml.jackson.databind.JsonNode;     // NOVO IMPORT

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

@Component
public class EventConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LaboratorioService laboratorioService;

    private Connection connection;
    private Channel channel;

    public EventConsumer(LaboratorioService laboratorioService) {
        this.laboratorioService = laboratorioService;
    }

    @PostConstruct
    public void iniciar() {
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
                connection = factory.newConnection();
                channel = connection.createChannel();

                channel.exchangeDeclare(RabbitMqConfig.EXCHANGE_EVENTOS, BuiltinExchangeType.TOPIC, true);
                channel.queueDeclare(RabbitMqConfig.FILA_LABORATORIO, true, false, false, null);
                channel.queueBind(RabbitMqConfig.FILA_LABORATORIO, RabbitMqConfig.EXCHANGE_EVENTOS,
                        RabbitMqConfig.ROUTING_KEY_WILDCARD);

                channel.basicConsume(RabbitMqConfig.FILA_LABORATORIO, true,
                        (consumerTag, delivery) -> {
                            try {
                                String routingKey = delivery.getEnvelope().getRoutingKey();
                                JsonNode payloadNode = objectMapper.readTree(delivery.getBody());

                                // MONTA O ENVELOPE BASEADO NO QUE CHEGOU
                                EventoGateway evento = new EventoGateway();
                                evento.setPayload(payloadNode);
                                evento.setLaboratorio(payloadNode.path("lab").asText(null));

                                // Extrai o ID flexível (telemetria envia "id", alertas enviam "dispositivo" ou "pc")
                                String dispId = payloadNode.path("id").asText(
                                        payloadNode.path("dispositivo").asText(
                                                payloadNode.path("pc").asText(null)));
                                evento.setDispositivoId(dispId);
                                evento.setTimestampGateway(Instant.now());

                                // TRADUZ A ROUTING KEY PARA O TIPO DE EVENTO DO SERVICE
                                if (routingKey.equals(RabbitMqConfig.RK_TELEMETRIA_PC)) evento.setTipoEvento("STATUS_PC");
                                else if (routingKey.equals(RabbitMqConfig.RK_TELEMETRIA_AC)) evento.setTipoEvento("STATUS_AC");
                                else if (routingKey.equals(RabbitMqConfig.RK_TELEMETRIA_PROJETOR)) evento.setTipoEvento("STATUS_PROJETOR");
                                else if (routingKey.equals(RabbitMqConfig.RK_METRICA_AGREGADA)) evento.setTipoEvento("METRICA_AGREGADA");
                                else if (routingKey.startsWith("alerta")) {
                                    evento.setTipoEvento("ALERTA");
                                    evento.setDescricao(payloadNode.path("alerta").asText("Alerta desconhecido"));
                                }

                                laboratorioService.processar(evento);
                            } catch (Exception e) {
                                System.err.println("[ms-laboratorio] Erro ao processar evento: "
                                        + e.getMessage());
                            }
                        },
                        consumerTag -> System.out.println("[ms-laboratorio] Consumer cancelado: "
                                + consumerTag));

                System.out.println("[ms-laboratorio] Consumindo eventos de "
                        + RabbitMqConfig.FILA_LABORATORIO);
                break; // Conectou com sucesso, sai do loop

            } catch (Exception e) {
                tentativas++;
                System.err.printf("[ms-laboratorio] Falha ao conectar ao RabbitMQ (%d/%d): %s. Tentando novamente em 5s...%n",
                        tentativas, maxTentativas, e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @PreDestroy
    public void encerrar() {
        try {
            if (channel != null && channel.isOpen()) channel.close();
            if (connection != null && connection.isOpen()) connection.close();
        } catch (IOException | TimeoutException e) {
            System.err.println("[ms-laboratorio] Erro ao encerrar RabbitMQ: " + e.getMessage());
        }
    }
}
