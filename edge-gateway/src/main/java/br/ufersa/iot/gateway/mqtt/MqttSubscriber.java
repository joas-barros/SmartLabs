package br.ufersa.iot.gateway.mqtt;

import br.ufersa.iot.gateway.analysis.DataAnalyzer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import config.Config;
import model.AirConditioningDTO;
import model.PCDTO;
import model.ProjectorDTO;
import org.eclipse.paho.client.mqttv3.*;

public class MqttSubscriber implements MqttCallback, AutoCloseable {

    private final MqttClient client;
    private final DataAnalyzer dataAnalyzer;
    private final ObjectMapper objectMapper;

    public MqttSubscriber(DataAnalyzer dataAnalyzer) throws MqttException {
        this.dataAnalyzer = dataAnalyzer;
        this.objectMapper = new ObjectMapper();
        // Permite serializar/deserializar campos Instant do Java Time
        this.objectMapper.registerModule(new JavaTimeModule());

        // Conecta ao broker MQTT
        this.client = new MqttClient(Config.BROKER_MQTT_URL, "EdgeGateway-MQTT", null);
        this.client.setCallback(this);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);

        System.out.println("[MQTT-RX] Conectando ao Broker MQTT: " + Config.BROKER_MQTT_URL);
        client.connect(options);
        System.out.println("[MQTT-RX] Conectado com sucesso!");

        // Subscreve nos tópicos de telemetria e alertas do Lab 1
        // lab/+/pc/+/data
        // lab/+/ac/data
        // lab/+/projector/data
        // lab/+/alerts
        client.subscribe("lab/+/pc/+/data", 0);
        client.subscribe("lab/+/ac/data", 0);
        client.subscribe("lab/+/projector/data", 0);
        client.subscribe("lab/+/alerts", 1); // QoS 1 para alertas
        
        System.out.println("[MQTT-RX] Inscrito nos tópicos: lab/+/pc/+/data, lab/+/ac/data, lab/+/projector/data, lab/+/alerts");
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[MQTT-RX] Conexão com o broker MQTT perdida: " + cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        byte[] payload = message.getPayload();
        try {
            if (topic.contains("/pc/")) {
                PCDTO pc = objectMapper.readValue(payload, PCDTO.class);
                
                // Realiza as verificações na borda (Gateway)
                if (pc.getTemperature() != null && pc.getCpu() != null) {
                    if (pc.getTemperature() > 85.0 || pc.getCpu() > 95.0) {
                        publishMqttAlert(pc, "SUPERAQUECIMENTO");
                    }
                }
                if (Boolean.TRUE.equals(pc.getSecurityEventDetected())) {
                    publishMqttAlert(pc, "SEGURANÇA");
                }

                dataAnalyzer.processPC(pc);
            } else if (topic.contains("/ac/")) {
                AirConditioningDTO ac = objectMapper.readValue(payload, AirConditioningDTO.class);

                // Realiza as verificações na borda (Gateway)
                if (ac.getEnvironmentTemperature() != null && (ac.getIsOn() == null || !ac.getIsOn())) {
                    if (ac.getEnvironmentTemperature() > 30.0) {
                        publishMqttAcAlert(ac, "FALHA_INFRAESTRUTURA");
                    }
                }

                dataAnalyzer.processAC(ac);
            } else if (topic.contains("/projector/")) {
                ProjectorDTO proj = objectMapper.readValue(payload, ProjectorDTO.class);
                dataAnalyzer.processProjector(proj);
            } else if (topic.contains("/alerts")) {
                JsonNode json = objectMapper.readTree(payload);
                String lab = json.has("lab") ? json.get("lab").asText() : "Desconhecido";
                String dispositivo = "Desconhecido";
                if (json.has("dispositivo")) {
                    dispositivo = json.get("dispositivo").asText();
                } else if (json.has("pc")) {
                    dispositivo = json.get("pc").asText();
                }
                String alerta = json.has("alerta") ? json.get("alerta").asText() : "Desconhecido";
                dataAnalyzer.processExternalAlert(lab, dispositivo, alerta);
            } else {
                System.out.printf("[MQTT-RX] Tópico desconhecido recebido: %s%n", topic);
            }
        } catch (Exception e) {
            System.err.printf("[MQTT-RX] Erro ao processar mensagem do tópico %s: %s%n", topic, e.getMessage());
        }
    }

    private void publishMqttAlert(PCDTO pc, String alertType) {
        try {
            // Formata o JSON do alerta de PC exatamente como solicitado
            String alertJson = String.format(
                "{\"lab\":\"%s\",\"pc\":\"%s\",\"cpu\":%d,\"temperatura\":%d,\"alerta\":\"%s\"}",
                pc.getLab(),
                pc.getId(),
                pc.getCpu() != null ? pc.getCpu().intValue() : 0,
                pc.getTemperature() != null ? pc.getTemperature().intValue() : 0,
                alertType
            );
            MqttMessage message = new MqttMessage(alertJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            message.setQos(1);
            String topic = Config.alertTopic(pc.getLab());
            client.publish(topic, message);
            System.out.printf("[MQTT-RX-ALERT] Alerta de PC publicado em %s: %s%n", topic, alertJson);
        } catch (Exception e) {
            System.err.println("[MQTT-RX] Falha ao publicar alerta de PC: " + e.getMessage());
        }
    }

    private void publishMqttAcAlert(AirConditioningDTO ac, String alertType) {
        try {
            String alertJson = String.format(
                "{\"lab\":\"%s\",\"dispositivo\":\"%s\",\"alerta\":\"%s\"}",
                ac.getLab(),
                ac.getId(),
                alertType
            );
            MqttMessage message = new MqttMessage(alertJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            message.setQos(1);
            String topic = Config.alertTopic(ac.getLab());
            client.publish(topic, message);
            System.out.printf("[MQTT-RX-ALERT] Alerta de AC publicado em %s: %s%n", topic, alertJson);
        } catch (Exception e) {
            System.err.println("[MQTT-RX] Falha ao publicar alerta de AC: " + e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Não envia dados, apenas recebe
    }

    @Override
    public void close() throws Exception {
        if (client.isConnected()) {
            client.disconnect();
        }
        client.close();
        System.out.println("[MQTT-RX] Conector MQTT finalizado.");
    }
}
