package br.ufersa.iot.gateway;

import br.ufersa.iot.gateway.analysis.DataAnalyzer;
import br.ufersa.iot.gateway.cloud.CloudConnector;
import br.ufersa.iot.gateway.coap.CoapSubscriber;
import br.ufersa.iot.gateway.mqtt.MqttSubscriber;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.UdpConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EdgeGateway implements AutoCloseable {

    private final CloudConnector cloudConnector;
    private final DataAnalyzer dataAnalyzer;
    private final ScheduledExecutorService scheduler;
    
    private MqttSubscriber mqttReceiver;
    private CoapSubscriber coapReceiver;
    private volatile boolean running = true;

    public EdgeGateway() {
        System.out.println("=========================================================");
        System.out.println("   INICIANDO GATEWAY DE BORDA (EDGE GATEWAY LAYER)       ");
        System.out.println("=========================================================\n");

        this.cloudConnector = new CloudConnector();
        this.dataAnalyzer = new DataAnalyzer(cloudConnector);
        this.scheduler = Executors.newScheduledThreadPool(3);

        startReceivers();
        startSchedulers();
        startConsoleListener();
    }

    private void startReceivers() {
        try {
            this.mqttReceiver = new MqttSubscriber(dataAnalyzer);
        } catch (Exception e) {
            System.err.println("[ERRO] Falha ao iniciar receptor MQTT. Certifique-se de que o Broker MQTT está executando.");
            e.printStackTrace();
        }

        try {
            this.coapReceiver = new CoapSubscriber(dataAnalyzer);
        } catch (Exception e) {
            System.err.println("[ERRO] Falha ao iniciar servidor CoAP.");
            e.printStackTrace();
        }
    }

    private void startSchedulers() {
        // 1. Verifica falhas de conectividade dos dispositivos a cada 3 segundos
        scheduler.scheduleAtFixedRate(
                dataAnalyzer::checkConnectivityFailures, 
                3, 3, TimeUnit.SECONDS
        );

        // 2. Verifica sobrecarga do laboratório a cada 4 segundos
        scheduler.scheduleAtFixedRate(
                dataAnalyzer::checkLabOverloadRules, 
                4, 4, TimeUnit.SECONDS
        );

        // 3. Calcula agregados locais e envia à nuvem a cada 10 segundos
        scheduler.scheduleAtFixedRate(
                dataAnalyzer::aggregateAndSendMetrics, 
                10, 10, TimeUnit.SECONDS
        );

        System.out.println("[GATEWAY] Agendadores inicializados: Verificação de Conectividade, Sobrecarga e Agregações Periódicas.");
    }

    private void startConsoleListener() {
        Thread thread = new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("\n[Console CLI] Digite 'help' para ver os comandos interativos disponíveis.");
            while (running) {
                try {
                    System.out.print("> ");
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    processCommand(line.trim().toLowerCase());
                } catch (Exception e) {
                    System.err.println("Erro ao ler comando do console: " + e.getMessage());
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void processCommand(String cmd) {
        switch (cmd) {
            case "help" -> {
                System.out.println("\n--- Comandos do Edge Gateway ---");
                System.out.println("  online   - Define a conexão com a nuvem como ONLINE.");
                System.out.println("  offline  - Define a conexão com a nuvem como OFFLINE (cacheamento local).");
                System.out.println("  status   - Exibe o estado atual da nuvem, cache e tabela de gêmeos digitais.");
                System.out.println("  help     - Exibe este menu de ajuda.");
                System.out.println("  exit     - Finaliza o Gateway.");
                System.out.println("--------------------------------\n");
            }
            case "online" -> cloudConnector.setOnline(true);
            case "offline" -> cloudConnector.setOnline(false);
            case "status" -> printStatus();
            case "exit" -> {
                System.out.println("Finalizando Edge Gateway...");
                try {
                    close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }
            default -> System.out.println("Comando inválido. Digite 'help' para a lista de comandos.");
        }
    }

    private void printStatus() {
        System.out.println("\n=========================================================");
        System.out.println("               ESTADO DO EDGE GATEWAY                    ");
        System.out.println("=========================================================");
        System.out.printf("  Nuvem: %s%n", cloudConnector.isOnline() ? "ONLINE" : "OFFLINE (Modo Cache Local)");
        System.out.printf("  Mensagens no Cache Offline: %d%n", cloudConnector.getCacheSize());
        System.out.println("---------------------------------------------------------");
        System.out.println("  Gêmeos Digitais Locais (Estado dos Dispositivos):");
        
        Map<String, Map<String, DataAnalyzer.DeviceState>> twins = dataAnalyzer.getTwins();
        for (String lab : twins.keySet()) {
            System.out.printf("  [%s]:%n", lab);
            Map<String, DataAnalyzer.DeviceState> labDevices = twins.get(lab);
            if (labDevices.isEmpty()) {
                System.out.println("    Nenhum dispositivo cadastrado.");
            } else {
                for (String id : labDevices.keySet()) {
                    DataAnalyzer.DeviceState state = labDevices.get(id);
                    System.out.printf("    - %s (%s): %s (Último reporte: %s)%n", 
                            id, state.type, state.isOnline ? "ONLINE" : "FALHA_CONECTIVIDADE (OFFLINE)", state.lastSeen);
                }
            }
        }
        System.out.println("=========================================================\n");
    }

    @Override
    public void close() throws Exception {
        running = false;
        scheduler.shutdown();
        if (mqttReceiver != null) {
            mqttReceiver.close();
        }
        if (coapReceiver != null) {
            coapReceiver.close();
        }
        System.out.println("Edge Gateway fechado.");
    }

    public static void main(String[] args) {
        CoapConfig.register();
        UdpConfig.register();
        try (EdgeGateway gateway = new EdgeGateway()) {
            // Mantém a thread principal ativa
            while (gateway.running) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.err.println("Erro na execução principal do Edge Gateway:");
            e.printStackTrace();
        }
    }
}
