package br.ufersa.iot.gateway.cloud;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CloudConnector {

    private volatile boolean online = true;
    private final Queue<String> offlineCache = new ConcurrentLinkedQueue<>();
    private final ExecutorService recoveryExecutor = Executors.newSingleThreadExecutor();

    public CloudConnector() {
        System.out.println("[CLOUD] Conector de Nuvem Inicializado (Estado: ONLINE).");
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
        String message = String.format("{\"type\":\"%s\", \"payload\":%s}", type, payload);
        if (online) {
            // Simula envio enviando diretamente para a nuvem
            System.out.printf("[CLOUD-TX] Enviando dados para o backend (%s): %s%n", type, payload);
        } else {
            // Modo offline: armazena no buffer local
            offlineCache.add(message);
            System.out.printf("[CLOUD-OFFLINE] Sem conexão. Dados cacheados localmente (%s): %s (Fila: %d)%n", 
                    type, payload, offlineCache.size());
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
                    String msg = offlineCache.peek();
                    if (msg != null) {
                        // Simula tempo de rede para enviar
                        Thread.sleep(100); 
                        System.out.printf("[CLOUD-TX-RECOVERY] Sincronizado: %s%n", msg);
                        offlineCache.poll();
                    }
                }
                if (offlineCache.isEmpty()) {
                    System.out.println("[CLOUD-RECOVERY] Sincronização concluída com sucesso! Cache local vazio.");
                } else {
                    System.out.printf("[CLOUD-RECOVERY] Sincronização interrompida (Gateway ficou offline novamente). Restaram %d mensagens.%n", 
                            offlineCache.size());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[CLOUD-RECOVERY] Sincronização interrompida.");
            }
        });
    }

    public int getCacheSize() {
        return offlineCache.size();
    }
}
