package coap;

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

public class CoapOrchestrator {

    public static void run() {
        System.out.println("=========================================================");
        System.out.println("   INICIANDO SIMULADOR LAB 2 - CoAP (AUTÔNOMO)           ");
        System.out.println("=========================================================\n");

        System.out.println("[LAB 2] A aguardar 5 segundos pela inicialização do Edge Gateway...");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Instancia o publicador CoAP apontando para o Gateway
        CoapPublisher coapPublisher = new CoapPublisher(Config.GATEWAY_COAP_URL);

        ScenarioManager sm = new ScenarioManager();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(15);

        System.out.println("[LAB 2] Inicializando dispositivos com Jitter...");
        setupDevices(scheduler, coapPublisher, sm);

        System.out.println("\n[LAB 2] Iniciando Roteiro da Simulação...\n");
        scheduleSimulation(scheduler, sm);
    }

    private static void setupDevices(ScheduledExecutorService scheduler, CoapPublisher coap, ScenarioManager sm) {
        for (int i = 1; i <= Config.PCS_PER_LAB; i++) {
            String pcId = "PC" + String.format("%02d", i);

            Consumer<PCDTO> edgeProcessorPC = dto -> {
                // 1. Envia telemetria normal (equivalente a QoS 0, mensagem NON)
                coap.publish(Config.pcCoapResource(pcId), dto);

                // 2. Analisa os dados localmente e dispara alerta se necessário
                if (dto.getTemperature() > 85.0 || dto.getCpu() > 95.0) {
                    String alertJson = String.format("{\"lab\":\"%s\",\"dispositivo\":\"%s\",\"alerta\":\"SUPERAQUECIMENTO\"}", dto.getLab(), dto.getId());
                    coap.publishAlert(Config.alertCoapResource(), alertJson);
                }

                if (Boolean.TRUE.equals(dto.getSecurityEventDetected())) {
                    String alertJson = String.format("{\"lab\":\"%s\",\"dispositivo\":\"%s\",\"alerta\":\"SEGURANÇA\"}", dto.getLab(), dto.getId());
                    coap.publishAlert(Config.alertCoapResource(), alertJson);
                }
            };

            PCSimulator pc = new PCSimulator(Config.LAB2, pcId, sm, edgeProcessorPC);
            // Uso do Jitter: Atraso inicial aleatório entre 0 e 2000 milissegundos
            scheduler.scheduleAtFixedRate(pc, jitter(), Config.PC_INTERVAL_SEC * 1000L, TimeUnit.MILLISECONDS);
        }

        Consumer<AirConditioningDTO> edgeProcessorAC = dto -> {
            coap.publish(Config.acCoapResource(), dto);

            if (!dto.getIsOn() && dto.getEnvironmentTemperature() > 30.0) {
                String alertJson = String.format("{\"lab\":\"%s\",\"dispositivo\":\"%s\",\"alerta\":\"FALHA_INFRAESTRUTURA\"}", dto.getLab(), dto.getId());
                coap.publishAlert(Config.alertCoapResource(), alertJson);
            }
        };
        ACSimulator ac = new ACSimulator(Config.LAB2, "AC01", sm, edgeProcessorAC);
        scheduler.scheduleAtFixedRate(ac, jitter(), Config.AIR_CONDITIONING_INTERVAL_SEC * 1000L, TimeUnit.MILLISECONDS);

        // Projetor não possui regra de alerta específica neste caso
        Consumer<ProjectorDTO> projConsumer = dto -> coap.publish(Config.projectorCoapResource(), dto);
        ProjectorSimulator projetor = new ProjectorSimulator(Config.LAB2, "PROJ01", sm, projConsumer);
        scheduler.scheduleAtFixedRate(projetor, jitter(), Config.PROJECTOR_INTERVAL_SEC * 1000L, TimeUnit.MILLISECONDS);
    }

    private static void scheduleSimulation(ScheduledExecutorService scheduler, ScenarioManager sm) {
        List<ScenarioDuration> roteiro = List.of(
                new ScenarioDuration(ScenarioType.NORMAL_USE, 45),             // 45 seg normal
                new ScenarioDuration(ScenarioType.OVERLOAD, 75),               // 1 min e 15 seg sobrecarga (rede saturada)
                new ScenarioDuration(ScenarioType.PEAK_USE, 60),               // 1 min prova em laboratório
                new ScenarioDuration(ScenarioType.NORMAL_USE, 60),             // 1 min descanso e resfriamento
                new ScenarioDuration(ScenarioType.INFRASTRUCTURE_FAILURE, 60)  // 1 min falha de infra na reta final
        );

        Iterator<ScenarioDuration> it = roteiro.iterator();
        scheduleNextFase(scheduler, sm, it);
    }

    private static void scheduleNextFase(ScheduledExecutorService scheduler, ScenarioManager sm, Iterator<ScenarioDuration> it) {
        if (it.hasNext()) {
            ScenarioDuration fase = it.next();

            System.out.println("\n=================================================");
            System.out.printf(" >>[LAB@] INICIANDO CÁPITULO: %s (Duração: %ds)%n", fase.scenario(), fase.duration());
            System.out.println("=================================================\n");

            sm.setCurrentScenario(fase.scenario());

            // Agenda a RECURSÃO para quando o tempo desta fase acabar
            scheduler.schedule(() -> scheduleNextFase(scheduler, sm, it), fase.duration(), TimeUnit.SECONDS);
        } else {
            // Quando a lista acabar, a simulação encerra graciosamente
            System.out.println("\n=================================================");
            System.out.println("   FIM DO ROTEIRO (5 MINUTOS). ENCERRANDO LAB 1. ");
            System.out.println("=================================================");
            scheduler.shutdown();

            // O Californium CoapClient fecha a conexão internamente nas requisições do Publisher,
            // então não precisamos forçar o fechamento de um cliente persistente aqui como no MQTT.
            System.exit(0);
        }
    }

    /**
     * Retorna um tempo aleatório entre 0 e 2000 milissegundos
     * para evitar que todas as threads disparem no mesmo instante exato.
     */
    private static long jitter() {
        return (long) (Math.random() * 2000);
    }

}
