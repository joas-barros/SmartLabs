package model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record AirConditioningDTO(
        String lab,
        String id,
        Double environmentTemperature,
        Boolean isOn,
        Double powerConsumptionInWatts,
        OperationModeAC operationModeAC,

        @JsonProperty("timestamp")
        Instant timestamp
) {

}
