package mqtt;

import config.Config;
import devices.ACSimulator;
import devices.PCSimulator;
import devices.ProjectorSimulator;
import model.AirConditioningDTO;
import model.PCDTO;
import model.ProjectorDTO;
import scenario.ScenarioDuration;
import scenario.ScenarioManager;
import scenario.ScenarioType;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MqttOrchestrator {

    public static void run() {
        System.out.println("=========================================================");
        System.out.println("   INICIANDO SIMULADOR LAB 1 - MQTT (EDGE & TWINS)       ");
        System.out.println("=========================================================\n");

        MqttPublisher mqttPublisher = null;

        ScenarioManager sm = new ScenarioManager();
        try {
            mqttPublisher = new MqttPublisher(Config.BROKER_MQTT_URL, "Lab1-mqtt");

        } catch (Exception e) {
            System.err.println("[ERRO CRÍTICO] Falha ao conectar no Broker MQTT. Verifique se o Mosquitto está rodando.");
            e.printStackTrace();
            System.exit(1);
        }

        // Pool de threads exclusivo para os dispositivos do Lab 1
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(15);

        System.out.println("[LAB 1] Inicializando dispositivos...");
        setupDevices(scheduler, mqttPublisher, sm);

        System.out.println("\n[LAB 1] Iniciando Roteiro da Simulação...\n");
        scheduleSimulation(scheduler, sm, mqttPublisher);
    }

    private static void scheduleSimulation(ScheduledExecutorService scheduler, ScenarioManager sm, MqttPublisher mqtt) {
        List<ScenarioDuration> roteiro = List.of(
                new ScenarioDuration(ScenarioType.NORMAL_USE, 60),             // 1 min normal
                new ScenarioDuration(ScenarioType.PEAK_USE, 90),               // 1.5 min aula cheia
                new ScenarioDuration(ScenarioType.INFRASTRUCTURE_FAILURE, 60), // 1 min ar pifou (ficar de olho nos alertas!)
                new ScenarioDuration(ScenarioType.ANOMALOUS_BEHAVIOR, 60),     // 1 min invasão (alertas de segurança)
                new ScenarioDuration(ScenarioType.NORMAL_USE, 30)              // 30 seg volta ao normal
        );

        Iterator<ScenarioDuration> it = roteiro.iterator();
        scheduleNextFase(scheduler, sm, it, mqtt);
    }

    private static void scheduleNextFase(ScheduledExecutorService scheduler, ScenarioManager sm, Iterator<ScenarioDuration> it, MqttPublisher mqtt) {
        if (it.hasNext()) {
            ScenarioDuration fase = it.next();

            System.out.println("\n=================================================");
            System.out.printf(" >> INICIANDO CÁPITULO: %s (Duração: %ds)%n", fase.scenario(), fase.duration());
            System.out.println("=================================================\n");

            sm.setCurrentScenario(fase.scenario());

            // Agenda a RECURSÃO para quando o tempo desta fase acabar
            scheduler.schedule(() -> scheduleNextFase(scheduler, sm, it, mqtt), fase.duration(), TimeUnit.SECONDS);
        } else {
            // Quando a lista acabar, a simulação encerra graciosamente
            System.out.println("\n=================================================");
            System.out.println("   FIM DO ROTEIRO (5 MINUTOS). ENCERRANDO LAB 1. ");
            System.out.println("=================================================");
            scheduler.shutdown();
            try { mqtt.close(); } catch (Exception ignored) {}
            System.exit(0);
        }
    }

    private static void setupDevices(ScheduledExecutorService scheduler, MqttPublisher mqtt, ScenarioManager sm) {
        // Inicializa os 10 PCs do LAB 1
        for (int i = 1; i <= Config.PCS_PER_LAB; i++) {
            String pcId = "PC" + String.format("%02d", i);

            Consumer<PCDTO> mqttConsumer = dto -> {
                mqtt.publish(Config.pcTopic(Config.LAB1, pcId), dto);
            };

            PCSimulator pc = new PCSimulator(Config.LAB1, pcId, sm, mqttConsumer);
            // Uso do Jitter: Atraso inicial aleatório entre 0 e 2000 milissegundos
            scheduler.scheduleAtFixedRate(pc, jitter(), Config.PC_INTERVAL_SEC * 1000L, TimeUnit.MILLISECONDS);
        }

        // Inicializa o Ar-Condicionado do LAB 1
        Consumer<AirConditioningDTO> acConsumer = dto -> {
            mqtt.publish(Config.acTopic(Config.LAB1), dto);
        };
        ACSimulator ac = new ACSimulator(Config.LAB1, "AC01", sm, acConsumer);
        scheduler.scheduleAtFixedRate(ac, jitter(), Config.AIR_CONDITIONING_INTERVAL_SEC * 1000L, TimeUnit.MILLISECONDS);

        // Inicializa o Projetor do LAB 1
        Consumer<ProjectorDTO> projConsumer = dto -> mqtt.publish(Config.projectorTopic(Config.LAB1), dto);
        ProjectorSimulator projetor = new ProjectorSimulator(Config.LAB1, "PROJ01", sm, projConsumer);
        scheduler.scheduleAtFixedRate(projetor, jitter(), Config.PROJECTOR_INTERVAL_SEC * 1000L, TimeUnit.MILLISECONDS);
    }

    /**
     * Retorna um tempo aleatório entre 0 e 2000 milissegundos
     * para evitar que todas as threads disparem no mesmo instante exato.
     */
    private static long jitter() {
        return (long) (Math.random() * 2000);
    }
}
