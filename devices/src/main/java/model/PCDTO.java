package model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class PCDTO {

    private String lab;
    private String id;
    private Double cpu;
    private Double ram;
    private Double temperature;
    private PCState status;
    private String applicationInUse;
    private Double networkUsageInMbps;
    private Boolean securityEventDetected;
    private String securityEventDescription;

    @JsonProperty("timestamp")
    private Instant timestamp;

    public PCDTO() {
    }

    public PCDTO(String lab, String id) {
        this.lab = lab;
        this.id = id;
        this.timestamp = Instant.now();
    }

    // Getters e Setters


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

    public Double getCpu() {
        return cpu;
    }

    public void setCpu(Double cpu) {
        this.cpu = cpu;
    }

    public Double getRam() {
        return ram;
    }

    public void setRam(Double ram) {
        this.ram = ram;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public PCState getStatus() {
        return status;
    }

    public void setStatus(PCState status) {
        this.status = status;
    }

    public String getApplicationInUse() {
        return applicationInUse;
    }

    public void setApplicationInUse(String applicationInUse) {
        this.applicationInUse = applicationInUse;
    }

    public Double getNetworkUsageInMbps() {
        return networkUsageInMbps;
    }

    public void setNetworkUsageInMbps(Double networkUsageInMbps) {
        this.networkUsageInMbps = networkUsageInMbps;
    }

    public Boolean getSecurityEventDetected() {
        return securityEventDetected;
    }

    public void setSecurityEventDetected(Boolean securityEventDetected) {
        this.securityEventDetected = securityEventDetected;
    }

    public String getSecurityEventDescription() {
        return securityEventDescription;
    }

    public void setSecurityEventDescription(String securityEventDescription) {
        this.securityEventDescription = securityEventDescription;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        String securityAlert = (securityEventDetected != null && securityEventDetected)
                ? "⚠️ ALERTA: " + securityEventDescription
                : "✅ Seguro";

        return String.format(
                """
                
                [COMPUTADOR] %s @ %s
                ├─ Status   : %s
                ├─ CPU/RAM  : %.1f%% / %.1f%%
                ├─ Temp.    : %.1f °C
                ├─ App Uso  : %s
                ├─ Rede     : %.2f Mbps
                ├─ Segurança: %s
                └─ Data/Hora: %s\
                """,
                id, lab, status, cpu, ram, temperature,
                applicationInUse, networkUsageInMbps,
                securityAlert, timestamp
        );
    }
}
