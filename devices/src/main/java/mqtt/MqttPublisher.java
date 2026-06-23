package mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttPublisher implements AutoCloseable {

    private final MqttClient client;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    public MqttPublisher(String brokerUrl, String clientId) throws MqttException {
        this.client = new MqttClient(brokerUrl, clientId, null);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);

        client.connect(options);
        System.out.printf("[MQTT] Conectado ao broker %s com clientId=%s%n", brokerUrl, clientId);
    }

    /**
     * Publica um objeto serializado em JSON no tópico informado, com QoS 0
     * (adequado para dados de telemetria de alta frequência).
     */
    public void publish(String topic, Object payload) {
        try {
            // CORREÇÃO: Evita dupla serialização se o payload já for uma String (JSON formatado)
            byte[] jsonBytes = payload instanceof String
                    ? ((String) payload).getBytes(java.nio.charset.StandardCharsets.UTF_8)
                    : objectMapper.writeValueAsBytes(payload);

            MqttMessage message = new MqttMessage(jsonBytes);
            message.setQos(0);
            client.publish(topic, message);
            System.out.println("[MQTT] Publicado messagem em " + topic);
        } catch (Exception e) {
            System.err.printf("[MQTT] Falha ao publicar em %s: %s%n", topic, e.getMessage());
        }
    }

    /**
     * Publica com QoS 1, recomendado para alertas críticos que não devem ser perdidos.
     */
    public void publishAlert(String topic, Object payload) {
        try {
            // CORREÇÃO: Evita dupla serialização
            byte[] jsonBytes = payload instanceof String
                    ? ((String) payload).getBytes(java.nio.charset.StandardCharsets.UTF_8)
                    : objectMapper.writeValueAsBytes(payload);

            MqttMessage message = new MqttMessage(jsonBytes);
            message.setQos(1);
            client.publish(topic, message);
            System.out.println("[MQTT] Publicado alerta em " + topic);
        } catch (Exception e) {
            System.err.printf("[MQTT] Falha ao publicar em %s: %s%n", topic, e.getMessage());
        }
    }

    @Override
    public void close() throws Exception {
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        } catch (MqttException e) {
            System.err.printf("[MQTT] Erro ao desconectar: %s%n", e.getMessage());
        }
    }
}
