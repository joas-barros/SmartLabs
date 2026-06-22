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
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
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
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(RabbitMqConfig.HOST);
            factory.setPort(RabbitMqConfig.PORT);
            factory.setUsername(RabbitMqConfig.USER);
            factory.setPassword(RabbitMqConfig.PASS);
            factory.setAutomaticRecoveryEnabled(true);

            connection = factory.newConnection();
            channel = connection.createChannel();

            channel.exchangeDeclare(RabbitMqConfig.EXCHANGE_EVENTOS, BuiltinExchangeType.TOPIC, true);
            channel.queueDeclare(RabbitMqConfig.FILA_LABORATORIO, true, false, false, null);
            channel.queueBind(RabbitMqConfig.FILA_LABORATORIO, RabbitMqConfig.EXCHANGE_EVENTOS,
                    RabbitMqConfig.ROUTING_KEY_WILDCARD);

            channel.basicConsume(RabbitMqConfig.FILA_LABORATORIO, true,
                    (consumerTag, delivery) -> {
                        try {
                            EventoGateway evento = objectMapper.readValue(
                                    delivery.getBody(), EventoGateway.class);
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

        } catch (IOException | TimeoutException e) {
            System.err.println("[ms-laboratorio] Falha ao conectar ao RabbitMQ: " + e.getMessage());
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
