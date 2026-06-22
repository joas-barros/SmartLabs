package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class ProjectorDTO {

    private String lab;
    private String id;
    private Boolean isOn;
    private Long usageTimeInMinutes;
    private Double internalTemperature;
    private String activeVideoInput;
    private Double powerConsumptionInWatts;

    @JsonProperty("timestamp")
    private Instant timestamp;

    public ProjectorDTO() {
    }

    public ProjectorDTO(String id, String lab) {
        this.id = id;
        this.lab = lab;
        this.timestamp = Instant.now();
    }

    // Construtor com todos os argumentos
    public ProjectorDTO(String lab, String id, Boolean isOn, Long usageTimeInMinutes,
                        Double internalTemperature, String activeVideoInput,
                        Double powerConsumptionInWatts, Instant timestamp) {
        this.lab = lab;
        this.id = id;
        this.isOn = isOn;
        this.usageTimeInMinutes = usageTimeInMinutes;
        this.internalTemperature = internalTemperature;
        this.activeVideoInput = activeVideoInput;
        this.powerConsumptionInWatts = powerConsumptionInWatts;
        this.timestamp = timestamp;
    }

    // --- Getters e Setters ---

    public String getLab() {
        return lab;
    }

    public void setLab(String lab) {
        this.lab = lab;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getIsOn() {
        return isOn;
    }

    public void setIsOn(Boolean isOn) {
        this.isOn = isOn;
    }

    public Long getUsageTimeInMinutes() {
        return usageTimeInMinutes;
    }

    public void setUsageTimeInMinutes(Long usageTimeInMinutes) {
        this.usageTimeInMinutes = usageTimeInMinutes;
    }

    public Double getInternalTemperature() {
        return internalTemperature;
    }

    public void setInternalTemperature(Double internalTemperature) {
        this.internalTemperature = internalTemperature;
    }

    public String getActiveVideoInput() {
        return activeVideoInput;
    }

    public void setActiveVideoInput(String activeVideoInput) {
        this.activeVideoInput = activeVideoInput;
    }

    public Double getPowerConsumptionInWatts() {
        return powerConsumptionInWatts;
    }

    public void setPowerConsumptionInWatts(Double powerConsumptionInWatts) {
        this.powerConsumptionInWatts = powerConsumptionInWatts;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return String.format(
                """
                
                [PROJETOR] %s @ %s
                ├─ Estado   : %s
                ├─ Entrada  : %s
                ├─ Uso      : %d min
                ├─ Temp.    : %.1f °C
                ├─ Consumo  : %.1f W
                └─ Data/Hora: %s\
                """,
                id, lab, (isOn != null && isOn ? "LIGADO" : "DESLIGADO"),
                activeVideoInput, usageTimeInMinutes,
                internalTemperature, powerConsumptionInWatts, timestamp
        );
    }
}