package br.ufersa.iot.gateway;

import br.ufersa.iot.gateway.analysis.DataAnalyzer;
import br.ufersa.iot.gateway.cloud.CloudConnector;
import br.ufersa.iot.gateway.coap.CoapSubscriber;
import br.ufersa.iot.gateway.mqtt.MqttSubscriber;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.UdpConfig;

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
        this.dataAnalyzer = new DataAnalyzer(this.cloudConnector);
        this.scheduler = Executors.newScheduledThreadPool(3);

        startReceivers();
        startSchedulers();
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

    // Console listener and command processor are removed to keep the Edge Gateway layer independent.

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

        if (cloudConnector != null) {
            cloudConnector.close();
        }
        System.out.println("Edge Gateway fechado.");
    }

    static void main(String[] args) {
        config.LoggerConfig.setup("gateway.log");
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
