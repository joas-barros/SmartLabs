package br.ufersa.iot.backend.ms_laboratorio.service;

import br.ufersa.iot.backend.ms_laboratorio.model.EstadoProcessado;
import br.ufersa.iot.backend.ms_laboratorio.model.EventoGateway;
import br.ufersa.iot.backend.ms_laboratorio.model.RegistroHistorico;
import br.ufersa.iot.backend.ms_laboratorio.repository.EstadoProcessadoRepository;
import br.ufersa.iot.backend.ms_laboratorio.repository.HistoricoRepository;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LaboratorioService {

    private static final Pattern PADRAO_INTERVALO = Pattern.compile("^(\\d+)([hm])$");

    /** Limiar de CPU média na janela para considerar "CPU alta persistente". */
    private static final double LIMIAR_CPU_PERSISTENTE = 85.0;

    /** Percentual mínimo de PCs em sobrecarga para detectar sobrecarga do laboratório. */
    private static final double LIMIAR_SOBRECARGA = 0.7;

    private final HistoricoRepository historicoRepository;
    private final EstadoProcessadoRepository estadoRepository;

    public LaboratorioService(HistoricoRepository historicoRepository,
                              EstadoProcessadoRepository estadoRepository) {
        this.historicoRepository = historicoRepository;
        this.estadoRepository = estadoRepository;
    }

    // ══════════════════════════════════════════════════════════
    // Processamento contínuo (chamado pelo consumidor RabbitMQ)
    // ══════════════════════════════════════════════════════════

    /**
     * Ponto de entrada do processamento contínuo.
     * Chamado pelo consumidor RabbitMQ a cada evento recebido.
     */
    public void processar(EventoGateway evento) {
        if (evento.getTipoEvento() == null || evento.getLaboratorio() == null) return;

        registrarNoHistorico(evento);

        switch (evento.getTipoEvento()) {
            case "STATUS_PC"    -> processarStatusPc(evento);
            case "STATUS_AC"    -> processarStatusAc(evento);
            case "METRICA_AGREGADA" -> processarMetricaAgregada(evento);
            default             -> { /* alertas e status de projetor: só histórico */ }
        }
    }

    private void registrarNoHistorico(EventoGateway evento) {
        JsonNode dados = evento.getPayload();
        Double cpu = null, ram = null, temperatura = null;

        if (dados != null) {
            if (dados.has("cpu"))               cpu         = dados.path("cpu").asDouble();
            if (dados.has("ram"))               ram         = dados.path("ram").asDouble();

            // CORREÇÃO DAS CHAVES JSON COM BASE NOS DTOS REAIS
            if (dados.has("temperature"))       temperatura = dados.path("temperature").asDouble();
            else if (dados.has("environmentTemperature"))  temperatura = dados.path("environmentTemperature").asDouble();
            else if (dados.has("internalTemperature"))   temperatura = dados.path("internalTemperature").asDouble();
        }

        historicoRepository.adicionar(new RegistroHistorico(
                Instant.now(),
                evento.getLaboratorio(),
                evento.getDispositivoId(),
                evento.getTipoEvento(),
                cpu, ram, temperatura,
                evento.getDescricao()
        ));
    }

    /**
     * Processa a telemetria individual de um PC:
     * atualiza médias móveis e detecta padrões temporais.
     */
    private void processarStatusPc(EventoGateway evento) {
        JsonNode dados = evento.getPayload();
        if (dados == null || evento.getDispositivoId() == null) return;

        String lab = evento.getLaboratorio();
        String dispositivoId = evento.getDispositivoId();
        EstadoProcessado estado = estadoRepository.obterOuCriar(lab);

        double cpu         = dados.path("cpu").asDouble();
        // CORREÇÃO: "temperature" em vez de "temperatura"
        double temperatura = dados.path("temperature").asDouble();

        double mediaCpu  = estado.registrarCpu(dispositivoId, cpu);
        double mediaTemp = estado.registrarTemperatura(dispositivoId, temperatura);

        estado.setUltimaAtualizacao(Instant.now());

        // Detecção de CPU alta persistente
        boolean cpuPersistente = estado.cpuAltaPersistente(dispositivoId);
        List<String> cpuAlta = new ArrayList<>(estado.getDispositivosCpuAltaPersistente());
        if (cpuPersistente && !cpuAlta.contains(dispositivoId)) {
            cpuAlta.add(dispositivoId);
            estado.setDispositivosCpuAltaPersistente(cpuAlta);
            estado.registrarPadraoDetectado(
                    String.format("CPU alta persistente em %s/%s: média=%.1f%% nas últimas %d leituras",
                            lab, dispositivoId, mediaCpu, EstadoProcessado.TAMANHO_JANELA));
        } else if (!cpuPersistente && cpuAlta.contains(dispositivoId)) {
            cpuAlta.remove(dispositivoId);
            estado.setDispositivosCpuAltaPersistente(cpuAlta);
        }

        // Detecção de tendência de temperatura crescente
        boolean tempCrescente = estado.temperaturaEmTendenciaCrescente(dispositivoId);
        List<String> tempCresc = new ArrayList<>(estado.getDispositivosTemperaturaCrescente());
        if (tempCrescente && !tempCresc.contains(dispositivoId)) {
            tempCresc.add(dispositivoId);
            estado.setDispositivosTemperaturaCrescente(tempCresc);
            estado.registrarPadraoDetectado(
                    String.format("Tendência de aquecimento em %s/%s: subiu ≥5°C na janela (média=%.1f°C)",
                            lab, dispositivoId, mediaTemp));
        } else if (!tempCrescente && tempCresc.contains(dispositivoId)) {
            tempCresc.remove(dispositivoId);
            estado.setDispositivosTemperaturaCrescente(tempCresc);
        }

        // Correlação: CPU alta E temperatura alta ao mesmo tempo neste PC
        atualizarCorrelacaoSuperaquecimento(estado);

        // Sobrecarga do laboratório
        atualizarSobrecarga(estado);
    }

    /**
     * Processa o status do AC: se desligado, registra padrão de possível
     * falha de infraestrutura.
     */
    private void processarStatusAc(EventoGateway evento) {
        JsonNode dados = evento.getPayload();
        if (dados == null) return;

        // CORREÇÃO: "isOn" e "environmentTemperature"
        boolean ligado = dados.path("isOn").asBoolean(true);
        if (!ligado) {
            EstadoProcessado estado = estadoRepository.obterOuCriar(evento.getLaboratorio());
            double tempAmbiente = dados.path("environmentTemperature").asDouble();
            estado.registrarPadraoDetectado(
                    String.format("AC de %s desligado — temperatura ambiente: %.1f°C",
                            evento.getLaboratorio(), tempAmbiente));
        }
    }

    /**
     * Processa métricas agregadas enviadas pelo gateway.
     */
    private void processarMetricaAgregada(EventoGateway evento) {
        JsonNode dados = evento.getPayload();
        if (dados == null) return;

        EstadoProcessado estado = estadoRepository.obterOuCriar(evento.getLaboratorio());

        // AQUI ESTÁ CORRETO: O Gateway formata o JSON agregado com "cpu_media"
        double mediaCpu = dados.path("cpu_media").asDouble();
        if (mediaCpu >= 85.0) {
            estado.registrarPadraoDetectado(
                    String.format("Média de CPU do laboratório %s elevada: %.1f%%",
                            evento.getLaboratorio(), mediaCpu));
        }
    }

    private void atualizarCorrelacaoSuperaquecimento(EstadoProcessado estado) {
        int superaquecidos = 0;
        for (Map.Entry<String, Double> entry : estado.getMediasCpu().entrySet()) {
            String dispositivoId = entry.getKey();
            double mediaCpu  = entry.getValue();
            double mediaTemp = estado.getMediasTemperatura().getOrDefault(dispositivoId, 0.0);
            if (mediaCpu >= 85.0 && mediaTemp >= 75.0) superaquecidos++;
        }
        estado.setPcsSuperaquecimento(superaquecidos);
        if (superaquecidos > 0) {
            estado.registrarPadraoDetectado(
                    String.format("Correlação superaquecimento em %s: %d PC(s) com CPU e temperatura altas simultaneamente",
                            estado.getLaboratorio(), superaquecidos));
        }
    }

    private void atualizarSobrecarga(EstadoProcessado estado) {
        int totalPcs = estado.getMediasCpu().size();
        if (totalPcs == 0) return;

        long pcsComCpuAlta = estado.getMediasCpu().values().stream()
                .filter(m -> m >= LIMIAR_CPU_PERSISTENTE)
                .count();

        boolean emSobrecarga = ((double) pcsComCpuAlta / totalPcs) >= LIMIAR_SOBRECARGA;

        if (emSobrecarga && !estado.isEmSobrecarga()) {
            estado.setEmSobrecarga(true);
            estado.registrarPadraoDetectado(
                    String.format("SOBRECARGA em %s: %d/%d PCs com CPU média ≥ %.0f%%",
                            estado.getLaboratorio(), pcsComCpuAlta, totalPcs, LIMIAR_CPU_PERSISTENTE));
        } else if (!emSobrecarga && estado.isEmSobrecarga()) {
            estado.setEmSobrecarga(false);
        }
    }

    // ══════════════════════════════════════════════════════════
    // Consultas sob demanda (chamadas pelos controllers REST)
    // ══════════════════════════════════════════════════════════

    public List<RegistroHistorico> historicoLaboratorio(String laboratorio, String intervalo) {
        return historicoRepository.buscarDesde(laboratorio.toUpperCase(), calcularInicio(intervalo));
    }

    public List<RegistroHistorico> historicoDispositivo(String laboratorio, String dispositivoId, String intervalo) {
        return historicoRepository.buscarPorDispositivo(
                laboratorio.toUpperCase(), dispositivoId, calcularInicio(intervalo));
    }

    public EstadoProcessado estadoProcessado(String laboratorio) {
        return estadoRepository.obterOuCriar(laboratorio.toUpperCase());
    }

    public Map<String, Object> estatisticas(String laboratorio, String intervalo) {
        List<RegistroHistorico> registros = historicoLaboratorio(laboratorio, intervalo);

        List<RegistroHistorico> leiturasPc = registros.stream()
                .filter(r -> "STATUS_PC".equals(r.getTipoEvento()) && r.getCpu() != null)
                .toList();

        if (leiturasPc.isEmpty()) {
            return Map.of(
                    "laboratorio", laboratorio.toUpperCase(),
                    "intervalo", intervalo != null ? intervalo : "24h",
                    "totalRegistros", registros.size(),
                    "mensagem", "Nenhuma leitura de PC no intervalo"
            );
        }

        double mediaCpu  = leiturasPc.stream().map(RegistroHistorico::getCpu)
                .filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(0);
        double maxCpu    = leiturasPc.stream().map(RegistroHistorico::getCpu)
                .filter(Objects::nonNull).mapToDouble(Double::doubleValue).max().orElse(0);
        double mediaRam  = leiturasPc.stream().map(RegistroHistorico::getRam)
                .filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(0);
        double maxRam    = leiturasPc.stream().map(RegistroHistorico::getRam)
                .filter(Objects::nonNull).mapToDouble(Double::doubleValue).max().orElse(0);
        double mediaTemp = leiturasPc.stream().map(RegistroHistorico::getTemperatura)
                .filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(0);
        double maxTemp   = leiturasPc.stream().map(RegistroHistorico::getTemperatura)
                .filter(Objects::nonNull).mapToDouble(Double::doubleValue).max().orElse(0);

        long totalAlertas = registros.stream()
                .filter(r -> r.getDescricao() != null && !r.getDescricao().isBlank())
                .count();

        EstadoProcessado estado = estadoRepository.obterOuCriar(laboratorio.toUpperCase());

        return Map.of(
                "laboratorio", laboratorio.toUpperCase(),
                "intervalo", intervalo != null ? intervalo : "24h",
                "totalRegistros", registros.size(),
                "totalLeiturasPc", leiturasPc.size(),
                "totalRegistrosComDescricao", totalAlertas,
                "cpu", Map.of("media", round(mediaCpu), "maximo", round(maxCpu)),
                "ram", Map.of("media", round(mediaRam), "maximo", round(maxRam)),
                "temperatura", Map.of("media", round(mediaTemp), "maximo", round(maxTemp)),
                "estadoAtual", Map.of(
                        "emSobrecarga", estado.isEmSobrecarga(),
                        "pcsSuperaquecimento", estado.getPcsSuperaquecimento(),
                        "dispositivosCpuAltaPersistente", estado.getDispositivosCpuAltaPersistente(),
                        "dispositivosTemperaturaCrescente", estado.getDispositivosTemperaturaCrescente()
                )
        );
    }

    private Instant calcularInicio(String intervalo) {
        if (intervalo == null || intervalo.isBlank()) return Instant.now().minus(Duration.ofHours(24));
        Matcher m = PADRAO_INTERVALO.matcher(intervalo.trim().toLowerCase());
        if (!m.matches()) return Instant.now().minus(Duration.ofHours(24));
        long valor = Long.parseLong(m.group(1));
        Duration d = m.group(2).equals("h") ? Duration.ofHours(valor) : Duration.ofMinutes(valor);
        return Instant.now().minus(d);
    }

    private double round(double v) { return Math.round(v * 10) / 10.0; }
}