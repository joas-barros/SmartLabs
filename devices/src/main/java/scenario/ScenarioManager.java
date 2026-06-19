package scenario;

import java.util.concurrent.atomic.AtomicReference;

public class ScenarioManager {
    private final AtomicReference<ScenarioType> currentScenario =
            new AtomicReference<>(ScenarioType.NORMAL_USE);

    public ScenarioManager() {
    }

    public ScenarioType getCurrentScenario() {
        return currentScenario.get();
    }

    public void setCurrentScenario(ScenarioType newScenario) {
        ScenarioType previous = currentScenario.getAndSet(newScenario);
        System.out.printf("[SCENARIO] Alterado %s para %s%n", previous, newScenario);
    }
}
