package br.ufersa.iot.backend.ms_laboratorio.controller;

import br.ufersa.iot.backend.ms_laboratorio.model.EstadoProcessado;
import br.ufersa.iot.backend.ms_laboratorio.model.RegistroHistorico;
import br.ufersa.iot.backend.ms_laboratorio.service.LaboratorioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class LabController {

    private final LaboratorioService service;

    public LabController(LaboratorioService service) {
        this.service = service;
    }

    /**
     * Histórico de eventos de um laboratório.
     * Exemplo: {@code GET /labs/LAB1/historico?intervalo=24h}
     */
    @GetMapping("/labs/{lab}/historico")
    public List<RegistroHistorico> historico(
            @PathVariable String lab,
            @RequestParam(defaultValue = "24h") String intervalo) {
        return service.historicoLaboratorio(lab, intervalo);
    }

    /**
     * Histórico de um dispositivo específico.
     * Exemplo: {@code GET /labs/LAB1/historico/PC01?intervalo=2h}
     */
    @GetMapping("/labs/{lab}/historico/{dispositivoId}")
    public List<RegistroHistorico> historicoDispositivo(
            @PathVariable String lab,
            @PathVariable String dispositivoId,
            @RequestParam(defaultValue = "24h") String intervalo) {
        return service.historicoDispositivo(lab, dispositivoId, intervalo);
    }

    /**
     * Estatísticas agregadas com estado de processamento atual.
     * Exemplo: {@code GET /labs/LAB1/estatisticas?intervalo=1h}
     */
    @GetMapping("/labs/{lab}/estatisticas")
    public Map<String, Object> estatisticas(
            @PathVariable String lab,
            @RequestParam(defaultValue = "24h") String intervalo) {
        return service.estatisticas(lab, intervalo);
    }

    /**
     * Estado atual do processamento contínuo: médias móveis, padrões
     * detectados, sobrecarga, correlações.
     * Exemplo: {@code GET /labs/LAB1/processamento}
     */
    @GetMapping("/labs/{lab}/processamento")
    public EstadoProcessado processamento(@PathVariable String lab) {
        return service.estadoProcessado(lab);
    }
}
