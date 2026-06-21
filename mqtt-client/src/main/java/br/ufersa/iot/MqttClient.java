package br.ufersa.iot;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MqttClient implements MqttCallback {

    private static final String BROKER_URL = "tcp://localhost:1883";
    private final org.eclipse.paho.client.mqttv3.MqttClient client;
    private final ObjectMapper objectMapper;
    private final Set<String> subscriptions = ConcurrentHashMap.newKeySet();

    public MqttClient() throws MqttException {
        String clientId = "MqttClientCLI-" + System.currentTimeMillis();
        this.client = new org.eclipse.paho.client.mqttv3.MqttClient(BROKER_URL, clientId, new MemoryPersistence());
        this.client.setCallback(this);
        this.objectMapper = new ObjectMapper();

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(10);

        System.out.println("[MQTT-CLI] Conectando ao Broker MQTT: " + BROKER_URL);
        this.client.connect(options);
        System.out.println("[MQTT-CLI] Conectado com sucesso!");
    }

    public void subscribe(String topic) {
        try {
            client.subscribe(topic, 0);
            subscriptions.add(topic);
            System.out.printf("[MQTT-CLI] Assinado com sucesso ao tópico: %s%n", topic);
        } catch (MqttException e) {
            System.err.printf("[MQTT-CLI] Erro ao assinar tópico %s: %s%n", topic, e.getMessage());
        }
    }

    public void unsubscribe(String topic) {
        try {
            if (subscriptions.contains(topic)) {
                client.unsubscribe(topic);
                subscriptions.remove(topic);
                System.out.printf("[MQTT-CLI] Assinatura cancelada para o tópico: %s%n", topic);
            } else {
                System.out.printf("[MQTT-CLI] Você não está assinado no tópico: %s%n", topic);
            }
        } catch (MqttException e) {
            System.err.printf("[MQTT-CLI] Erro ao cancelar assinatura de %s: %s%n", topic, e.getMessage());
        }
    }

    public void listSubscriptions() {
        if (subscriptions.isEmpty()) {
            System.out.println("[MQTT-CLI] Nenhuma assinatura ativa.");
        } else {
            System.out.println("[MQTT-CLI] Assinaturas ativas:");
            for (String sub : subscriptions) {
                System.out.println("  - " + sub);
            }
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[MQTT-CLI] Conexão com o broker MQTT perdida! Tentando reconectar...");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        System.out.println("\n--------------------------------------------------");
        System.out.printf("[EVENTO RECEBIDO] Tópico: %s%n", topic);
        System.out.println("Payload:");
        try {
            // Tenta formatar o JSON de forma legível (pretty print)
            Object json = objectMapper.readValue(payload, Object.class);
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            System.out.println(prettyJson);
        } catch (Exception e) {
            // Se o payload não for JSON, imprime como texto puro
            System.out.println(payload);
        }
        System.out.println("--------------------------------------------------");
        System.out.print("> "); // Reimprime o prompt
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Esse cliente apenas consome eventos
    }

    public void close() {
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
            System.out.println("[MQTT-CLI] Conexão encerrada.");
        } catch (MqttException e) {
            System.err.println("[MQTT-CLI] Erro ao encerrar conexão: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("   CLIENTE EM TEMPO REAL (MQTT) - SMARTLABS CLI          ");
        System.out.println("=========================================================");
        System.out.println("Comandos disponíveis:");
        System.out.println("  sub <topico>    - Assinar um tópico (ex: lab/+/alerts, lab/1/pc/+/cpu)");
        System.out.println("  unsub <topico>  - Cancelar assinatura de um tópico");
        System.out.println("  list            - Listar assinaturas ativas");
        System.out.println("  help            - Mostrar comandos disponíveis");
        System.out.println("  exit            - Sair");
        System.out.println("---------------------------------------------------------");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            MqttClient cli = new MqttClient();

            while (true) {
                System.out.print("> ");
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\s+", 2);
                String command = parts[0].toLowerCase();
                String argument = parts.length > 1 ? parts[1] : "";

                switch (command) {
                    case "exit" -> {
                        cli.close();
                        System.exit(0);
                    }
                    case "list" -> cli.listSubscriptions();
                    case "help" -> {
                        System.out.println("Comandos disponíveis:");
                        System.out.println("  sub <topico>    - Assinar um tópico");
                        System.out.println("  unsub <topico>  - Cancelar assinatura de um tópico");
                        System.out.println("  list            - Listar assinaturas ativas");
                        System.out.println("  exit            - Sair");
                    }
                    case "sub" -> {
                        if (argument.isEmpty()) {
                            System.out.println("[Erro] Uso correto: sub <topico>");
                        } else {
                            cli.subscribe(argument);
                        }
                    }
                    case "unsub" -> {
                        if (argument.isEmpty()) {
                            System.out.println("[Erro] Uso correto: unsub <topico>");
                        } else {
                            cli.unsubscribe(argument);
                        }
                    }
                    default -> {
                        // Facilidade de uso: se digitar o tópico diretamente sem comando "sub", assina
                        if (line.contains("/") || line.contains("+") || line.contains("#")) {
                            cli.subscribe(line);
                        } else {
                            System.out.println("Comando desconhecido. Digite 'help' para comandos ou digite um tópico direto.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro na execução do cliente MQTT: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
