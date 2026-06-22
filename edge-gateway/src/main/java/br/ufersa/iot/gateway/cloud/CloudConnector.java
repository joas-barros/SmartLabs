package br.ufersa.iot.gateway.cloud;

import br.ufersa.iot.gateway.config.RabbitmqConfig;
import br.ufersa.iot.gateway.rabbitmq.PublisherRabbitMq;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloudConnector {

    private volatile boolean online = true;
    private final Queue<CloudMessage> offlineCache = new ConcurrentLinkedQueue<>();
    private final ExecutorService recoveryExecutor = Executors.newSingleThreadExecutor();

    private final PublisherRabbitMq rabbitMqPublisher;

    private record CloudMessage(String type, String payload) {}

    public CloudConnector() {
        System.out.println("[CLOUD] Conector de Nuvem Inicializado (Estado: ONLINE).");
        // Injeta o CloudConnector no Publisher para que ele possa gerenciar o estado online/offline
        this.rabbitMqPublisher = new PublisherRabbitMq(this);
    }

    public boolean isOnline() {
        return online;
    }

    public synchronized void setOnline(boolean online) {
        if (this.online == online) {
            return;
        }
        this.online = online;
        if (online) {
            System.out.println("\n[CLOUD-STATUS] >>> Conexão com a nuvem restabelecida! <<<");
            triggerRecovery();
        } else {
            System.out.println("\n[CLOUD-STATUS] >>> Conexão com a nuvem PERDIDA! Operando em Modo Offline. <<<");
        }
    }

    /**
     * Envia um payload (JSON) para a nuvem.
     * Se estiver offline, armazena em cache.
     */
    public void send(String type, String payload) {
        String routingKey = determineRoutingKey(type, payload);
        CloudMessage msg = new CloudMessage(type, payload);
        if (online) {
            // Simula envio enviando diretamente para a nuvem
            System.out.printf("[CLOUD-TX] Enviando dados para o backend (%s): %s%n", type, payload);
            try {
                rabbitMqPublisher.publish(routingKey, payload);
            } catch (Exception e) {
                // Se der erro no exato instante do envio, assume que caiu e joga pro cache
                cacheMessage(msg);
                setOnline(false);
            }
        } else {
            // Modo offline: armazena no buffer local
            cacheMessage(msg);
            System.out.printf("[CLOUD-OFFLINE] Sem conexão. Dados cacheados localmente (%s): %s (Fila: %d)%n", 
                    type, payload, offlineCache.size());
        }
    }

    private void cacheMessage(CloudMessage msg) {
        if (offlineCache.size() < RabbitmqConfig.CAPACIDADE_FILA_OFFLINE) {
            offlineCache.add(msg);
            System.out.printf("[CLOUD-OFFLINE] Cacheado (%s). Fila: %d/%d%n",
                    msg.type(), offlineCache.size(), RabbitmqConfig.CAPACIDADE_FILA_OFFLINE);
        } else {
            System.err.println("[CLOUD-OFFLINE] ALERTA: Fila offline cheia! Mensagem descartada.");
        }
    }

    /**
     * Descarrega o cache offline em uma thread separada.
     */
    private void triggerRecovery() {
        recoveryExecutor.submit(() -> {
            int count = offlineCache.size();
            if (count == 0) return;

            System.out.printf("[CLOUD-RECOVERY] Iniciando sincronização de %d mensagens acumuladas...%n", count);
            try {
                while (!offlineCache.isEmpty() && online) {
                    CloudMessage msg = offlineCache.peek();
                    if (msg != null) {
                        String rk = determineRoutingKey(msg.type(), msg.payload());
                        rabbitMqPublisher.publish(rk, msg.payload());

                        Thread.sleep(50); // Delay sutil para não engasgar o RabbitMQ
                        System.out.printf("[CLOUD-TX-RECOVERY] Sincronizado: %s%n", rk);
                        offlineCache.poll(); // Remove apenas se tiver publicado com sucesso
                    }
                }
                if (offlineCache.isEmpty()) {
                    System.out.println("[CLOUD-RECOVERY] Sincronização concluída com sucesso! Cache local vazio.");
                } else {
                    System.out.printf("[CLOUD-RECOVERY] Sincronização interrompida. Restam %d mensagens no cache.%n", offlineCache.size());
                }
            } catch (Exception e) {
                System.err.println("[CLOUD-RECOVERY] Sincronização interrompida: " + e.getMessage());
                setOnline(false);
            }
        });
    }

    private String determineRoutingKey(String type, String payload) {
        return switch (type) {
            case "TELEMETRIA_PC" -> RabbitmqConfig.RK_TELEMETRIA_PC;
            case "TELEMETRIA_AC" -> RabbitmqConfig.RK_TELEMETRIA_AC;
            case "TELEMETRIA_PROJETOR" -> RabbitmqConfig.RK_TELEMETRIA_PROJETOR;
            case "ALERT", "EDGE_ALERT" -> payload.contains("SEGURANÇA") ?
                    RabbitmqConfig.RK_ALERTA_SEGURANCA :
                    RabbitmqConfig.RK_ALERTA_CRITICO;
            case "AGGREGATED_METRICS" -> RabbitmqConfig.RK_METRICA_AGREGADA;
            default -> "evento.desconhecido";
        };
    }

    public int getCacheSize() {
        return offlineCache.size();
    }

    public void close() {
        System.out.println("[CLOUD] Encerrando Conector de Nuvem...");
        recoveryExecutor.shutdown();
        try {
            if (rabbitMqPublisher != null) {
                rabbitMqPublisher.close();
            }
        } catch (Exception e) {
            System.err.println("[CLOUD] Erro ao fechar conexão com RabbitMQ: " + e.getMessage());
        }
    }
}
