package br.ufersa.iot.gateway.rabbitmq;

import br.ufersa.iot.gateway.cloud.CloudConnector;
import br.ufersa.iot.gateway.config.RabbitmqConfig;
import com.rabbitmq.client.*;

import java.io.IOException;

public class PublisherRabbitMq implements AutoCloseable{
    private Connection connection;
    private Channel channel;
    private final CloudConnector cloudConnector;

    public PublisherRabbitMq(CloudConnector cloudConnector) {
        this.cloudConnector = cloudConnector;
        connect();
    }

    private void connect() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RabbitmqConfig.RABBITMQ_HOST);
        factory.setPort(RabbitmqConfig.RABBITMQ_PORT);
        factory.setUsername(RabbitmqConfig.RABBITMQ_USER);
        factory.setPassword(RabbitmqConfig.RABBITMQ_PASS);

        // Habilita a recuperação automática de conexão nativa do RabbitMQ
        factory.setAutomaticRecoveryEnabled(true);

        int tentativas = 0;
        int maxTentativas = 10;

        while (tentativas < maxTentativas && (this.connection == null || !this.connection.isOpen())) {
            try {
                this.connection = factory.newConnection();
                this.channel = connection.createChannel();

                // Declara a exchange e as filas, e vincula por routing key
                channel.exchangeDeclare(RabbitmqConfig.EXCHANGE_EVENTOS, "topic", true);

                System.out.println("[RABBITMQ] Conectado com sucesso em " + RabbitmqConfig.RABBITMQ_HOST);
                cloudConnector.setOnline(true);

                // Ouvintes para detectar quedas e voltas da internet
                ((Recoverable) connection).addRecoveryListener(new RecoveryListener() {
                    @Override
                    public void handleRecovery(Recoverable recoverable) {
                        System.out.println("[RABBITMQ] Conexão restabelecida fisicamente pelo driver.");
                        cloudConnector.setOnline(true);
                    }
                    @Override
                    public void handleRecoveryStarted(Recoverable recoverable) {}
                });

                connection.addShutdownListener(cause -> {
                    if (!cause.isInitiatedByApplication()) {
                        System.err.println("[RABBITMQ] Perda de conexão física com o RabbitMQ!");
                        cloudConnector.setOnline(false);
                    }
                });

                break; // Sai do loop se conectou com sucesso

            } catch (Exception e) {
                tentativas++;
                System.err.printf("[RABBITMQ] Falha inicial ao conectar (%d/%d): %s. Tentando novamente em 5s...%n",
                        tentativas, maxTentativas, e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (this.connection == null || !this.connection.isOpen()) {
            System.err.println("[RABBITMQ] Desistindo de conectar após várias tentativas. Iniciando em modo offline.");
            cloudConnector.setOnline(false);
        }
    }

    public void publish(String routingKey, String payload) throws Exception {
        if (channel != null && channel.isOpen()) {
            channel.basicPublish(
                    RabbitmqConfig.EXCHANGE_EVENTOS,
                    routingKey,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    payload.getBytes("UTF-8")
            );
        } else {
            throw new IOException("Canal do RabbitMQ está fechado.");
        }
    }

    @Override
    public void close() throws Exception {
        if (channel != null && channel.isOpen()) channel.close();
        if (connection != null && connection.isOpen()) connection.close();
    }
}
