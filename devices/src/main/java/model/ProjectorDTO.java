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
}
