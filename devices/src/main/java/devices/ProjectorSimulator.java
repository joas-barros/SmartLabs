package devices;

import model.ProjectorDTO;
import scenario.ScenarioManager;
import scenario.ScenarioType;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class ProjectorSimulator implements Runnable{

    private static final List<String> VIDEO_INPUTS = List.of("HDMI1", "HDMI2", "VGA", "WIRELESS");

    private final String labId;
    private final String projetorId;
    private final Consumer<ProjectorDTO> publisher;

    private boolean isOn;
    private long timeUsedMinutes;
    private double internalTemperature;

    private final Random random = ThreadLocalRandom.current();

    public ProjectorSimulator(String labId, String projetorId, Consumer<ProjectorDTO> publisher) {
        this.labId = labId;
        this.projetorId = projetorId;
        this.publisher = publisher;

        this.isOn = false;
        this.timeUsedMinutes = 0;
        this.internalTemperature = 28 + random.nextDouble() * 4;
    }

    @Override
    public void run() {
        try {
            ScenarioType scenario = ScenarioManager.getCurrentScenario();
            ProjectorDTO data = new ProjectorDTO(labId, projetorId);

            switch (scenario) {
                case PEAK_USE -> generateOperationInClass(data, true);
                case NORMAL_USE -> generateOperationInClass(data, random.nextDouble() < 0.4);
                default -> generateOperationInClass(data, isOn); // mantém estado anterior
            }

            data.setInternalTemperature(round(internalTemperature));
            data.setUsageTimeInMinutes(timeUsedMinutes);
            publisher.accept(data);

        } catch (Exception e) {
            System.err.printf("[%s-%s] Erro ao gerar dados: %s%n", labId, projetorId, e.getMessage());
        }
    }

    private void generateOperationInClass(ProjectorDTO data, boolean shouldBeOn) {
        if (shouldBeOn) {
            timeUsedMinutes++;
            double ceilTemperature = 55 + random.nextDouble() * 5;
            internalTemperature += (ceilTemperature - internalTemperature) * 0.2 + (random.nextDouble() - 0.5) * 0.5;

            data.setIsOn(true);
            data.setActiveVideoInput(VIDEO_INPUTS.get(random.nextInt(VIDEO_INPUTS.size())));
            data.setPowerConsumptionInWatts(250 + random.nextDouble() * 50);

        } else {
            double temperatureAmbient = 26;
            internalTemperature += (temperatureAmbient - internalTemperature) * 0.15;

            data.setIsOn(false);
            data.setActiveVideoInput("NENHUMA");
            data.setPowerConsumptionInWatts(0.5); // standby
        }
    }

    private double round(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}
