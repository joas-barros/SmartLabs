package br.ufersa.iot.gateway.analysis;

import br.ufersa.iot.gateway.cloud.CloudConnector;
import model.AirConditioningDTO;
import model.PCDTO;
import model.PCState;
import model.ProjectorDTO;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataAnalyzer {

    private final CloudConnector cloudConnector;

    // Estrutura de dados para o Device Twin local dos dispositivos
    // LabId -> DeviceId -> DeviceState
    private final Map<String, Map<String, DeviceState>> twins = new ConcurrentHashMap<>();

    // Rastreamento de estados de alerta locais para evitar envio redundante de alertas
    private final Map<String, Long> activeAlerts = new ConcurrentHashMap<>();

    public DataAnalyzer(CloudConnector cloudConnector) {
        this.cloudConnector = cloudConnector;
        twins.put("LAB1", new ConcurrentHashMap<>());
        twins.put("LAB2", new ConcurrentHashMap<>());
    }

    public static class DeviceState {
        public final String type; // "PC", "AC", "PROJECTOR"
        public final Object dto;
        public final Instant lastSeen;
        public boolean isOnline;

        public DeviceState(String type, Object dto, Instant lastSeen) {
            this.type = type;
            this.dto = dto;
            this.lastSeen = lastSeen;
            this.isOnline = true;
        }
    }

    // ==========================================
    // MÉTODOS DE RECEPÇÃO E VALIDAÇÃO DE DADOS
    // ==========================================

    public void processPC(PCDTO pc) {
        if (!validatePC(pc)) {
            System.err.printf("[FILTRO] Telemetria de PC descartada (Dados Inválidos): Lab=%s, ID=%s%n", pc.getLab(), pc.getId());
            return;
        }

        String lab = pc.getLab();
        String id = pc.getId();

        // Atualiza Twin local
        Map<String, DeviceState> labDevices = twins.computeIfAbsent(lab, _ -> new ConcurrentHashMap<>());
        boolean wasOffline = false;
        DeviceState old = labDevices.get(id);
        if (old != null && !old.isOnline) {
            wasOffline = true;
        }
        labDevices.put(id, new DeviceState("PC", pc, Instant.now()));

        if (wasOffline) {
            triggerEdgeAlert(lab, id, "CONECTIVIDADE_RESTABELECIDA", 
                    String.format("{\"lab\":\"%s\",\"dispositivo\":\"%s\",\"info\":\"Voltou a reportar dados\"}", lab, id));
        }

        // Processamento local (Edge Rules)
        checkPCRules(pc);
    }

    public void processAC(AirConditioningDTO ac) {
        if (!validateAC(ac)) {
            System.err.printf("[FILTRO] Telemetria de Ar Condicionado descartada (Dados Inválidos): Lab=%s, ID=%s%n", ac.getLab(), ac.getId());
            return;
        }

        String lab = ac.getLab();
        String id = ac.getId();

        Map<String, DeviceState> labDevices = twins.computeIfAbsent(lab, _ -> new ConcurrentHashMap<>());
        labDevices.put(id, new DeviceState("AC", ac, Instant.now()));

        // Processamento local (Edge Rules)
        checkACRules(ac);
    }

    public void processProjector(ProjectorDTO proj) {
        if (!validateProjector(proj)) {
            System.err.printf("[FILTRO] Telemetria de Projetor descartada (Dados Inválidos): Lab=%s, ID=%s%n", proj.getLab(), proj.getId());
            return;
        }

        String lab = proj.getLab();
        String id = proj.getId();

        Map<String, DeviceState> labDevices = twins.computeIfAbsent(lab, _ -> new ConcurrentHashMap<>());
        labDevices.put(id, new DeviceState("PROJECTOR", proj, Instant.now()));

        // Processamento local (Edge Rules)
        checkProjectorRules(proj);
    }

    public void processExternalAlert(String lab, String dispositivo, String alerta) {
        System.out.printf("[ALERTA-DISPOSITIVO] Recebido alerta de dispositivo: Lab=%s, Disp=%s, Tipo=%s%n", lab, dispositivo, alerta);
        // Encaminha alerta crítico imediatamente para a nuvem
        String payload = String.format("{\"lab\":\"%s\",\"dispositivo\":\"%s\",\"alerta\":\"%s\",\"origem\":\"dispositivo\"}", lab, dispositivo, alerta);
        cloudConnector.send("ALERT", payload);
    }

    private void checkPCRules(PCDTO pc) {
        // Regra 1: CPU Alta (> 90%)
        if (pc.getCpu() != null && pc.getCpu() > 90.0) {
            String alertKey = pc.getLab() + "-" + pc.getId() + "-HIGH_CPU";
            if (shouldTriggerAlert(alertKey)) {
                triggerEdgeAlert(pc.getLab(), pc.getId(), "CPU_ALTA", 
                        String.format("{\"lab\":\"%s\",\"dispositivo\":\"%s\",\"cpu\":%.1f}", pc.getLab(), pc.getId(), pc.getCpu()));
            }
        }

        // Regra 2: Superaquecimento de PC (> 85°C)
        if (pc.getTemperature() != null && pc.getTemperature() > 85.0) {
            String alertKey = pc.getLab() + "-" + pc.getId() + "-OVERHEATING";
            if (shouldTriggerAlert(alertKey)) {
                triggerEdgeAlert(pc.getLab(), pc.getId(), "SUPERAQUECIMENTO_PC", 
                        String.format("{\"lab\":\"%s\",\"dispositivo\":\"%s\",\"temperatura\":%.1f}", pc.getLab(), pc.getId(), pc.getTemperature()));
            }
        }
    }

    private void checkACRules(AirConditioningDTO ac) {
        // Regra 3: Falha de Infraestrutura (Ar desligado com temperatura ambiente alta)
        if (ac.getEnvironmentTemperature() != null && ac.getEnvironmentTemperature() > 30.0 && (ac.getIsOn() == null || !ac.getIsOn())) {
            String alertKey = ac.getLab() + "-" + ac.getId() + "-TEMP_ALTA_DESLIGADO";
            if (shouldTriggerAlert(alertKey)) {
                triggerEdgeAlert(ac.getLab(), ac.getId(), "FALHA_INFRA_AR_DESLIGADO", 
                        String.format("{\"lab\":\"%s\",\"dispositivo\":\"%s\",\"temperatura_ambiente\":%.1f}", ac.getLab(), ac.getId(), ac.getEnvironmentTemperature()));
            }
        }
    }

    private void checkProjectorRules(ProjectorDTO proj) {
        // Regra 4: Superaquecimento do Projetor (> 80°C)
        if (proj.getInternalTemperature() != null && proj.getInternalTemperature() > 80.0) {
            String alertKey = proj.getLab() + "-" + proj.getId() + "-OVERHEATING";
            if (shouldTriggerAlert(alertKey)) {
                triggerEdgeAlert(proj.getLab(), proj.getId(), "SUPERAQUECIMENTO_PROJETOR", 
                        String.format("{\"lab\":\"%s\",\"dispositivo\":\"%s\",\"temperatura_interna\":%.1f}", proj.getLab(), proj.getId(), proj.getInternalTemperature()));
            }
        }
    }

    public void checkConnectivityFailures() {
        Instant now = Instant.now();
        for (String lab : twins.keySet()) {
            Map<String, DeviceState> labDevices = twins.get(lab);
            for (String id : labDevices.keySet()) {
                DeviceState state = labDevices.get(id);
                if (state.isOnline) {
                    long secondsSinceLastData = now.getEpochSecond() - state.lastSeen.getEpochSecond();
                    long threshold = state.type.equals("PC") ? 9 : 15; // 3x o intervalo normal

                    if (secondsSinceLastData > threshold) {
                        state.isOnline = false;
                        triggerEdgeAlert(lab, id, "FALHA_CONECTIVIDADE", 
                                String.format("{\"lab\":\"%s\",\"dispositivo\":\"%s\",\"segundos_sem_dados\":%d}", lab, id, secondsSinceLastData));
                    }
                }
            }
        }
    }

    public void checkLabOverloadRules() {
        for (String lab : twins.keySet()) {
            Map<String, DeviceState> labDevices = twins.get(lab);
            double totalCpu = 0.0;
            int activePcCount = 0;

            for (DeviceState state : labDevices.values()) {
                if (state.type.equals("PC") && state.isOnline) {
                    PCDTO pc = (PCDTO) state.dto;
                    if (pc.getStatus() == PCState.ACTIVE || pc.getStatus() == PCState.IN_TEST) {
                        totalCpu += pc.getCpu();
                        activePcCount++;
                    }
                }
            }

            if (activePcCount > 0) {
                double avgCpu = totalCpu / activePcCount;
                String alertKey = lab + "-OVERLOAD";
                if (avgCpu > 80.0) {
                    if (shouldTriggerAlert(alertKey)) {
                        triggerEdgeAlert(lab, "LAB", "SOBRECARGA_LABORATORIO", 
                                String.format("{\"lab\":\"%s\",\"cpu_media\":%.1f,\"pcs_ativos\":%d}", lab, avgCpu, activePcCount));
                    }
                } else {
                    // Se o alarme estava ativo e a sobrecarga acabou, remove o status de alerta
                    activeAlerts.remove(alertKey);
                }
            }
        }
    }

    private boolean shouldTriggerAlert(String alertKey) {
        Long lastAlert = activeAlerts.get(alertKey);
        long now = Instant.now().getEpochSecond();
        // Permite o mesmo tipo de alerta a cada 30 segundos no máximo para não inundar o backend
        if (lastAlert == null || (now - lastAlert) > 30) {
            activeAlerts.put(alertKey, now);
            return true;
        }
        return false;
    }

    private void triggerEdgeAlert(String lab, String dispositivo, String alerta, String payload) {
        System.out.printf("[EDGE-ALERTA-DETECTADO] >>> %s em %s (%s) <<<%n", alerta, lab, dispositivo);
        cloudConnector.send("EDGE_ALERT", payload);
    }

    // ==========================================
    // AGREGACAO DE METRICAS LOCAIS (PERIODICA)
    // ==========================================

    /**
     * Calcula as métricas agregadas por laboratório e as envia à nuvem.
     */
    public void aggregateAndSendMetrics() {
        twins.forEach((lab, labDevices) -> {
            if (labDevices.isEmpty()) return;

            // Filtra e mapeia os PCs online
            List<PCDTO> onlinePcs = labDevices.values().stream()
                    .filter(d -> d.isOnline && "PC".equals(d.type))
                    .map(d -> (PCDTO) d.dto)
                    .toList();

            long pcCount = onlinePcs.size();
            double avgCpu = onlinePcs.stream().mapToDouble(PCDTO::getCpu).average().orElse(0.0);
            double avgRam = onlinePcs.stream().mapToDouble(PCDTO::getRam).average().orElse(0.0);
            double avgTemp = onlinePcs.stream().mapToDouble(PCDTO::getTemperature).average().orElse(0.0);

            // Obtém o Ar Condicionado online
            AirConditioningDTO ac = labDevices.values().stream()
                    .filter(d -> d.isOnline && "AC".equals(d.type))
                    .map(d -> (AirConditioningDTO) d.dto)
                    .findFirst()
                    .orElse(null);

            Double acEnvironmentTemp = ac != null ? ac.getEnvironmentTemperature() : null;
            Boolean acOn = ac != null ? ac.getIsOn() : null;
            Double acPower = ac != null ? ac.getPowerConsumptionInWatts() : null;

            // Obtém o Projetor online
            ProjectorDTO proj = labDevices.values().stream()
                    .filter(d -> d.isOnline && "PROJECTOR".equals(d.type))
                    .map(d -> (ProjectorDTO) d.dto)
                    .findFirst()
                    .orElse(null);

            Boolean projOn = proj != null ? proj.getIsOn() : null;

            if (pcCount > 0 || acEnvironmentTemp != null || projOn != null) {
                String aggJson = String.format(
                        "{\"lab\":\"%s\",\"pcs_ativos\":%d,\"cpu_media\":%.1f,\"ram_media\":%.1f,\"temperatura_media_pc\":%.1f," +
                        "\"ar_ligado\":%s,\"temperatura_ambiente\":%s,\"ar_potencia_watts\":%s,\"projetor_ligado\":%s,\"timestamp\":\"%s\"}",
                        lab, pcCount, avgCpu, avgRam, avgTemp,
                        acOn, 
                        acEnvironmentTemp != null ? String.format("%.1f", acEnvironmentTemp) : "null",
                        acPower != null ? String.format("%.1f", acPower) : "null",
                        projOn,
                        Instant.now()
                );
                
                // Envia dados agregados filtrados
                cloudConnector.send("AGGREGATED_METRICS", aggJson);
            }
        });
    }

    // ==========================================
    // MÉTODOS AUXILIARES E DE VALIDAÇÃO
    // ==========================================

    private boolean validatePC(PCDTO pc) {
        if (pc == null || pc.getLab() == null || pc.getId() == null) return false;
        if (pc.getCpu() == null || pc.getCpu() < 0.0 || pc.getCpu() > 100.0) return false;
        if (pc.getRam() == null || pc.getRam() < 0.0 || pc.getRam() > 100.0) return false;
        return pc.getTemperature() != null && !(pc.getTemperature() < -50.0) && !(pc.getTemperature() > 150.0);
    }

    private boolean validateAC(AirConditioningDTO ac) {
        if (ac == null || ac.getLab() == null || ac.getId() == null) return false;
        if (ac.getEnvironmentTemperature() == null || ac.getEnvironmentTemperature() < -50.0 || ac.getEnvironmentTemperature() > 100.0) return false;
        return ac.getPowerConsumptionInWatts() == null || ac.getPowerConsumptionInWatts() >= 0;
    }

    private boolean validateProjector(ProjectorDTO proj) {
        if (proj == null || proj.getLab() == null || proj.getId() == null) return false;
        if (proj.getInternalTemperature() == null || proj.getInternalTemperature() < -50.0 || proj.getInternalTemperature() > 150.0) return false;
        return proj.getPowerConsumptionInWatts() == null || proj.getPowerConsumptionInWatts() >= 0;
    }

}
