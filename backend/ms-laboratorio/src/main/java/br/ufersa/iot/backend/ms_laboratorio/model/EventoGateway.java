package br.ufersa.iot.backend.ms_laboratorio.model;

import tools.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Espelha o envelope de evento publicado pelo gateway de borda.
 */
public class EventoGateway {

    private String tipoEvento;
    private String laboratorio;
    private String dispositivoId;
    private JsonNode payload;
    private String descricao;
    private Instant timestampGateway;

    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }

    public String getLaboratorio() { return laboratorio; }
    public void setLaboratorio(String laboratorio) { this.laboratorio = laboratorio; }

    public String getDispositivoId() { return dispositivoId; }
    public void setDispositivoId(String dispositivoId) { this.dispositivoId = dispositivoId; }

    public JsonNode getPayload() { return payload; }
    public void setPayload(JsonNode payload) { this.payload = payload; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Instant getTimestampGateway() { return timestampGateway; }
    public void setTimestampGateway(Instant timestampGateway) { this.timestampGateway = timestampGateway; }
}
