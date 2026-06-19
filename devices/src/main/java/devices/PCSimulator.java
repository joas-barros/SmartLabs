package devices;

import model.PCDTO;
import model.PCState;
import scenario.ScenarioManager;
import scenario.ScenarioType;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public class PCSimulator implements Runnable {

    private static final List<String> NORMAL_APLICATIONS = List.of(
            "vscode", "browser", "terminal", "libreoffice", "files", "intellij-idea"
    );

    private static final List<String> TEST_APLICATIONS = List.of(
            "test_enviroment", "blocked_browser", "visual_studio", "sigaa"
    );

    private static final List<String> NON_AUTHORIZED_SOFTWARES = List.of(
            "cripto-miner", "steam", "netflix", "p2p-sharing", "unauthorized-vpn"
    );

    private final String labId;
    private final String pcId;
    private final Consumer<PCDTO> publisher;
    private final ScenarioManager scenarioManager; // Recebido via injeção de dependência

    private double currentCpu;
    private double currentRam;
    private double currentTemperature;

    private final Random random = ThreadLocalRandom.current();

    // Construtor atualizado
    public PCSimulator(String labId, String pcId, ScenarioManager scenarioManager, Consumer<PCDTO> publisher) {
        this.labId = labId;
        this.pcId = pcId;
        this.scenarioManager = scenarioManager;
        this.publisher = publisher;

        this.currentCpu = 5 + random.nextDouble() * 10;
        this.currentRam = 20 + random.nextDouble() * 10;
        this.currentTemperature = 35 + random.nextDouble() * 5;
    }

    @Override
    public void run() {
        try {
            // Agora consulta a instância, e não a classe estática
            ScenarioType cenario = scenarioManager.getCurrentScenario();
            PCDTO data = new PCDTO(labId, pcId);

            switch (cenario) {
                case NORMAL_USE -> generateNormalUsage(data);
                case PEAK_USE -> generatePeakUsage(data);
                case INFRASTRUCTURE_FAILURE -> generateInfrastructureFailure(data);
                case OVERLOAD -> generateOverload(data);
                case ANOMALOUS_BEHAVIOR -> generateAnomalousBehavior(data);
            }

            data.setCpu(round(currentCpu));
            data.setRam(round(currentRam));
            data.setTemperature(round(currentTemperature));

            publisher.accept(data);

        } catch (Exception e) {
            System.err.printf("[%s-%s] Erro ao gerar dados: %s%n", labId, pcId, e.getMessage());
        }
    }

    // ───────────────────────────── Scenarios ─────────────────────────────
    private void generateNormalUsage(PCDTO data) {
        boolean active = random.nextDouble() < 0.6;

        double targetCpu = active ? (20 + random.nextDouble() * 30) : (3 + random.nextDouble() * 8);
        double targetRam = active ? (30 + random.nextDouble() * 25) : (10 + random.nextDouble() * 15);

        moveInDirection(targetCpu, targetRam, 1.0);
        adjustTemperaturePerCpu(45, 0.4);

        data.setStatus(active ? PCState.ACTIVE : PCState.IDLE);
        data.setApplicationInUse(choose(NORMAL_APLICATIONS));
        data.setNetworkUsageInMbps(active ? (1 + random.nextDouble() * 5) : (0.1 + random.nextDouble() * 0.5));
        data.setSecurityEventDetected(false);
    }

    private void generatePeakUsage(PCDTO data) {
        double targetCpu = 70 + random.nextDouble() * 25;
        double targetRam = 60 + random.nextDouble() * 30;

        moveInDirection(targetCpu, targetRam, 1.5);
        adjustTemperaturePerCpu(55, 0.6);

        data.setStatus(PCState.IN_TEST);
        data.setApplicationInUse(choose(TEST_APLICATIONS));
        data.setNetworkUsageInMbps(5 + random.nextDouble() * 10);
        data.setSecurityEventDetected(false);
    }

    private void generateInfrastructureFailure(PCDTO data) {
        boolean active = random.nextDouble() < 0.6;

        double targetCpu = active ? (20 + random.nextDouble() * 30) : (3 + random.nextDouble() * 8);
        double targetRam = active ? (30 + random.nextDouble() * 25) : (15 + random.nextDouble() * 10);

        moveInDirection(targetCpu, targetRam, 1.0);

        double ceilTemperature = 75 + (currentCpu * 0.3);
        if (currentTemperature < ceilTemperature) {
            currentTemperature += 0.8 + random.nextDouble() * 0.7;
        }

        data.setStatus(active ? PCState.ACTIVE : PCState.IDLE);
        data.setApplicationInUse(choose(NORMAL_APLICATIONS));
        data.setNetworkUsageInMbps(active ? (1 + random.nextDouble() * 5) : (0.1 + random.nextDouble() * 0.5));
        data.setSecurityEventDetected(false);
    }

    private void generateOverload(PCDTO data) {
        double targetCpu = 90 + random.nextDouble() * 10;
        double targetRam = 80 + random.nextDouble() * 18;

        moveInDirection(targetCpu, targetRam, 2.0);
        adjustTemperaturePerCpu(65, 0.8);

        data.setStatus(PCState.ACTIVE);
        data.setApplicationInUse(choose(NORMAL_APLICATIONS));
        data.setNetworkUsageInMbps(15 + random.nextDouble() * 20);
        data.setSecurityEventDetected(false);
    }

    private void generateAnomalousBehavior(PCDTO data) {
        boolean anomalous = random.nextDouble() < 0.2;

        if (anomalous) {
            double targetCpu = 80 + random.nextDouble() * 15;
            double targetRam = 70 + random.nextDouble() * 20;

            moveInDirection(targetCpu, targetRam, 1.8);
            adjustTemperaturePerCpu(60, 0.7);

            data.setStatus(PCState.ACTIVE);
            data.setApplicationInUse(choose(NON_AUTHORIZED_SOFTWARES));
            data.setNetworkUsageInMbps(10 + random.nextDouble() * 20);
            data.setSecurityEventDetected(true);
            data.setSecurityEventDescription("Execução de software não autorizado: "
                    + data.getApplicationInUse());
        } else {
            generateNormalUsage(data);
        }
    }

    // ───────────────────────────── Helpers ─────────────────────────────
    private void moveInDirection(double targetCpu, double targetRam, double factor) {
        double cpuStep = (targetCpu - currentCpu) * 0.3 + (random.nextDouble() - 0.5) * factor * 3;
        double ramStep = (targetRam - currentRam) * 0.2 + (random.nextDouble() - 0.5) * factor * 2;

        currentCpu = clamp(currentCpu + cpuStep, 0, 100);
        currentRam = clamp(currentRam + ramStep, 0, 100);
    }

    private void adjustTemperaturePerCpu(double ceilBase, double noise) {
        double ceilTemperature = ceilBase + (currentCpu * 0.35);
        double step = (ceilTemperature - currentTemperature) * 0.25 + (random.nextDouble() - 0.5) * noise;
        currentTemperature = clamp(currentTemperature + step, 25, 100);
    }

    private double clamp(double valor, double min, double max) {
        return Math.max(min, Math.min(max, valor));
    }

    private double round(double valor) {
        return Math.round(valor * 10) / 10.0;
    }

    private String choose(List<String> opcoes) {
        return opcoes.get(random.nextInt(opcoes.size()));
    }
}