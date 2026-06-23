package br.ufersa.iot.backend.ms_laboratorio.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Representa uma entrada no histórico de métricas/eventos de um dispositivo.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegistroHistorico {

    private Instant timestamp;
    private String laboratorio;
    private String dispositivoId;
    private String tipoEvento;
    private Double cpu;
    private Double ram;
    private Double temperatura;
    private String descricao;

    public RegistroHistorico() {
    }

    public RegistroHistorico(Instant timestamp, String laboratorio, String dispositivoId,
                             String tipoEvento, Double cpu, Double ram,
                             Double temperatura, String descricao) {
        this.timestamp = timestamp;
        this.laboratorio = laboratorio;
        this.dispositivoId = dispositivoId;
        this.tipoEvento = tipoEvento;
        this.cpu = cpu;
        this.ram = ram;
        this.temperatura = temperatura;
        this.descricao = descricao;
    }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getLaboratorio() { return laboratorio; }
    public void setLaboratorio(String laboratorio) { this.laboratorio = laboratorio; }

    public String getDispositivoId() { return dispositivoId; }
    public void setDispositivoId(String dispositivoId) { this.dispositivoId = dispositivoId; }

    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }

    public Double getCpu() { return cpu; }
    public void setCpu(Double cpu) { this.cpu = cpu; }

    public Double getRam() { return ram; }
    public void setRam(Double ram) { this.ram = ram; }

    public Double getTemperatura() { return temperatura; }
    public void setTemperatura(Double temperatura) { this.temperatura = temperatura; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}