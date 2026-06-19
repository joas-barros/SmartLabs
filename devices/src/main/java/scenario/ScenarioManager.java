package scenario;

import java.util.concurrent.atomic.AtomicReference;

public final class ScenarioManager {
    private static final AtomicReference<ScenarioType> currentScenario =
            new AtomicReference<>(ScenarioType.NORMAL_USE);

    private ScenarioManager() {
    }

    public static ScenarioType getCurrentScenario() {
        return currentScenario.get();
    }

    public static void setCurrentScenario(ScenarioType newScenario) {
        ScenarioType previous = currentScenario.getAndSet(newScenario);
        System.out.printf("[SCENARIO] Alterado %s para %s%n", previous, newScenario);
    }
}
