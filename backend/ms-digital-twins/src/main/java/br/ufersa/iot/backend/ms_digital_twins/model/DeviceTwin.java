package br.ufersa.iot.backend.ms_digital_twins.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeviceTwin {

    private String lab;
    private String deviceId;
    private String deviceType;
    private Instant lastUpdate;
    private boolean online;

    // Usando coleções Thread-Safe para evitar quebras no WebFlux vs RabbitMQ
    private Map<String, Object> state = new ConcurrentHashMap<>();
    private Deque<EventoHistorico> historicoEventos = new ConcurrentLinkedDeque<>();

    public DeviceTwin() {
    }

    public DeviceTwin(String lab, String deviceId, String deviceType) {
        this.lab = lab;
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.online = true;
        this.lastUpdate = Instant.now();
    }

    // Método utilitário para adicionar evento limitando o tamanho da fila a 15 (Evita OutOfMemory)
    public void addEvento(String descricao) {
        if (this.historicoEventos.size() >= 15) {
            this.historicoEventos.pollLast(); // Remove o mais antigo (do final)
        }
        this.historicoEventos.addFirst(new EventoHistorico(Instant.now(), descricao)); // Adiciona o mais novo no início
    }

    // --- Getters e Setters ---

    public String getLab() { return lab; }
    public void setLab(String lab) { this.lab = lab; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public Instant getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(Instant lastUpdate) { this.lastUpdate = lastUpdate; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public Map<String, Object> getState() { return state; }

    public void setState(Map<String, Object> state) {
        this.state.clear();
        if (state != null) {
            // O ConcurrentHashMap NÃO aceita valores nulos (daria NullPointerException)!
            // Portanto, iteramos e filtramos campos que vêm nulos no JSON
            // (como o "securityEventDescription": null)
            for (Map.Entry<String, Object> entry : state.entrySet()) {
                if (entry.getValue() != null) {
                    this.state.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public Deque<EventoHistorico> getHistoricoEventos() { return historicoEventos; }
    public void setHistoricoEventos(Deque<EventoHistorico> historicoEventos) {
        this.historicoEventos.clear();
        if (historicoEventos != null) {
            this.historicoEventos.addAll(historicoEventos);
        }
    }

    // --- Classe Aninhada de Evento ---

    public static class EventoHistorico {
        private Instant timestamp;
        private String descricao;

        public EventoHistorico() {
        }

        public EventoHistorico(Instant timestamp, String descricao) {
            this.timestamp = timestamp;
            this.descricao = descricao;
        }

        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

        public String getDescricao() { return descricao; }
        public void setDescricao(String descricao) { this.descricao = descricao; }
    }
}