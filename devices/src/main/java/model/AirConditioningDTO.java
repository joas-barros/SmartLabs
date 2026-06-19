package model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class AirConditioningDTO {

        private String lab;
        private String id;
        private Double environmentTemperature;
        private Boolean isOn;
        private Double powerConsumptionInWatts;
        private OperationModeAC operationModeAC;

        @JsonProperty("timestamp")
        private Instant timestamp;

        public AirConditioningDTO() {
        }

        public AirConditioningDTO(String lab, String id) {
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

        public Double getEnvironmentTemperature() {
                return environmentTemperature;
        }

        public void setEnvironmentTemperature(Double environmentTemperature) {
                this.environmentTemperature = environmentTemperature;
        }

        public Boolean getIsOn() {
                return isOn;
        }

        public void setIsOn(Boolean isOn) {
                this.isOn = isOn;
        }

        public Double getPowerConsumptionInWatts() {
                return powerConsumptionInWatts;
        }

        public void setPowerConsumptionInWatts(Double powerConsumptionInWatts) {
                this.powerConsumptionInWatts = powerConsumptionInWatts;
        }

        public OperationModeAC getOperationModeAC() {
                return operationModeAC;
        }

        public void setOperationModeAC(OperationModeAC operationModeAC) {
                this.operationModeAC = operationModeAC;
        }

        public Instant getTimestamp() {
                return timestamp;
        }

        public void setTimestamp(Instant timestamp) {
                this.timestamp = timestamp;
        }
}