package devices;

import model.AirConditioningDTO;
import model.OperationModeAC;
import scenario.ScenarioManager;
import scenario.ScenarioType;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class ACSimulator implements Runnable {
    private final String labId;
    private final String acId;
    private final Consumer<AirConditioningDTO> publisher;
    private final ScenarioManager scenarioManager; // Injeção

    private double environmentTemperature;
    private boolean isOn;

    private final Random random = ThreadLocalRandom.current();

    // Construtor atualizado
    public ACSimulator(String labId, String acId, ScenarioManager scenarioManager, Consumer<AirConditioningDTO> publisher) {
        this.labId = labId;
        this.acId = acId;
        this.scenarioManager = scenarioManager;
        this.publisher = publisher;

        this.environmentTemperature = 22 + random.nextDouble() * 3;
        this.isOn = true;
    }

    @Override
    public void run() {
        try {
            ScenarioType scenario = scenarioManager.getCurrentScenario(); // Uso da instância

            AirConditioningDTO data = new AirConditioningDTO(labId, acId);
            switch (scenario) {
                case INFRASTRUCTURE_FAILURE -> setFailure(data);
                case PEAK_USE, OVERLOAD -> setIntenseOperation(data);
                default -> setNormalOperation(data);
            }

            data.setEnvironmentTemperature(round(environmentTemperature));
            publisher.accept(data);

        } catch (Exception e) {
            System.err.printf("[%s-%s] Erro ao gerar dados: %s%n", labId, acId, e.getMessage());
        }
    }

    private void setNormalOperation(AirConditioningDTO data) {
        isOn = true;
        Double target = 22 + random.nextDouble() * 2;
        environmentTemperature += (target - environmentTemperature) * 0.3 + (random.nextDouble() - 0.5) * 0.3;

        data.setIsOn(true);
        data.setOperationModeAC(OperationModeAC.AUTOMATIC);
        data.setPowerConsumptionInWatts(800 + random.nextDouble() * 200);
    }

    private void setIntenseOperation(AirConditioningDTO data) {
        isOn = true;
        Double target = 21 + random.nextDouble() * 1.5;
        environmentTemperature += (target - environmentTemperature) * 0.25 + (random.nextDouble() - 0.5) * 0.4;

        data.setIsOn(true);
        data.setOperationModeAC(OperationModeAC.REFRIGERATE);
        data.setPowerConsumptionInWatts(1200 + random.nextDouble() * 300);
    }

    private void setFailure(AirConditioningDTO data) {
        isOn = false;
        Double root = 38 + random.nextDouble() * 4;

        if (environmentTemperature < root) {
            environmentTemperature += 0.5 + random.nextDouble() * 0.5;
        }

        data.setIsOn(false);
        data.setOperationModeAC(OperationModeAC.VENTILATE);
        data.setPowerConsumptionInWatts(0.0);
    }

    private double round(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}