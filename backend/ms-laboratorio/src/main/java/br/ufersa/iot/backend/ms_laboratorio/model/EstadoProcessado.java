package br.ufersa.iot.backend.ms_laboratorio.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EstadoProcessado {

    public static final int TAMANHO_JANELA = 10;

    private final String laboratorio;

    /** Janela deslizante de CPU por dispositivo. */
    private final ConcurrentHashMap<String, List<Double>> janelasCpu = new ConcurrentHashMap<>();

    /** Janela deslizante de temperatura por dispositivo. */
    private final ConcurrentHashMap<String, List<Double>> janelasTemperatura = new ConcurrentHashMap<>();

    /** Médias móveis atuais de CPU por dispositivo. */
    private final ConcurrentHashMap<String, Double> mediasCpu = new ConcurrentHashMap<>();

    /** Médias móveis atuais de temperatura por dispositivo. */
    private final ConcurrentHashMap<String, Double> mediasTemperatura = new ConcurrentHashMap<>();

    /** Dispositivos com CPU média persistentemente acima de 85% na janela. */
    private final List<String> dispositivosCpuAltaPersistente = new ArrayList<>();

    /** Dispositivos com temperatura em tendência crescente contínua. */
    private final List<String> dispositivosTemperaturaCrescente = new ArrayList<>();

    /** Quantidade de PCs com CPU alta E temperatura alta simultaneamente. */
    private volatile int pcsSuperaquecimento = 0;

    /** Indica se o laboratório está em estado de sobrecarga. */
    private volatile boolean emSobrecarga = false;

    /** Timestamp da última atualização do estado processado. */
    private volatile Instant ultimaAtualizacao = Instant.now();

    /** Últimos padrões detectados (para exibição via API). */
    private final List<String> ultimosPadroesDetectados = new ArrayList<>();
    private static final int MAX_PADROES = 20;

    public EstadoProcessado(String laboratorio) {
        this.laboratorio = laboratorio;
    }

    /**
     * Registra uma nova leitura de CPU para um dispositivo e atualiza
     * a média móvel correspondente. Retorna a média móvel atualizada.
     */
    public synchronized double registrarCpu(String dispositivoId, double valor) {
        List<Double> janela = janelasCpu.computeIfAbsent(dispositivoId, k -> new ArrayList<>());
        janela.add(valor);
        if (janela.size() > TAMANHO_JANELA) janela.remove(0);
        double media = janela.stream().mapToDouble(Double::doubleValue).average().orElse(valor);
        mediasCpu.put(dispositivoId, round(media));
        return media;
    }

    /**
     * Registra uma nova leitura de temperatura e atualiza a média móvel.
     * Retorna a média móvel atualizada.
     */
    public synchronized double registrarTemperatura(String dispositivoId, double valor) {
        List<Double> janela = janelasTemperatura.computeIfAbsent(dispositivoId, k -> new ArrayList<>());
        janela.add(valor);
        if (janela.size() > TAMANHO_JANELA) janela.remove(0);
        double media = janela.stream().mapToDouble(Double::doubleValue).average().orElse(valor);
        mediasTemperatura.put(dispositivoId, round(media));
        return media;
    }

    /**
     * Verifica se a temperatura de um dispositivo está em tendência de
     * crescimento contínuo na janela atual (subiu ≥ 5°C do primeiro ao
     * último elemento da janela, com janela cheia).
     */
    public synchronized boolean temperaturaEmTendenciaCrescente(String dispositivoId) {
        List<Double> janela = janelasTemperatura.get(dispositivoId);
        if (janela == null || janela.size() < TAMANHO_JANELA) return false;
        return (janela.get(janela.size() - 1) - janela.get(0)) >= 5.0;
    }

    /**
     * Verifica se a CPU de um dispositivo permanece acima de 85% em
     * todas as leituras da janela atual.
     */
    public synchronized boolean cpuAltaPersistente(String dispositivoId) {
        List<Double> janela = janelasCpu.get(dispositivoId);
        if (janela == null || janela.size() < TAMANHO_JANELA) return false;
        return janela.stream().allMatch(v -> v >= 85.0);
    }

    public synchronized void registrarPadraoDetectado(String descricao) {
        ultimosPadroesDetectados.add(0, Instant.now() + " | " + descricao);
        if (ultimosPadroesDetectados.size() > MAX_PADROES) {
            ultimosPadroesDetectados.remove(ultimosPadroesDetectados.size() - 1);
        }
    }

    // Getters e setters

    public String getLaboratorio() { return laboratorio; }

    public Map<String, Double> getMediasCpu() { return mediasCpu; }

    public Map<String, Double> getMediasTemperatura() { return mediasTemperatura; }

    public synchronized List<String> getDispositivosCpuAltaPersistente() {
        return List.copyOf(dispositivosCpuAltaPersistente);
    }

    public synchronized void setDispositivosCpuAltaPersistente(List<String> lista) {
        dispositivosCpuAltaPersistente.clear();
        dispositivosCpuAltaPersistente.addAll(lista);
    }

    public synchronized List<String> getDispositivosTemperaturaCrescente() {
        return List.copyOf(dispositivosTemperaturaCrescente);
    }

    public synchronized void setDispositivosTemperaturaCrescente(List<String> lista) {
        dispositivosTemperaturaCrescente.clear();
        dispositivosTemperaturaCrescente.addAll(lista);
    }

    public int getPcsSuperaquecimento() { return pcsSuperaquecimento; }
    public void setPcsSuperaquecimento(int v) { this.pcsSuperaquecimento = v; }

    public boolean isEmSobrecarga() { return emSobrecarga; }
    public void setEmSobrecarga(boolean emSobrecarga) { this.emSobrecarga = emSobrecarga; }

    public Instant getUltimaAtualizacao() { return ultimaAtualizacao; }
    public void setUltimaAtualizacao(Instant t) { this.ultimaAtualizacao = t; }

    public synchronized List<String> getUltimosPadroesDetectados() {
        return List.copyOf(ultimosPadroesDetectados);
    }

    private double round(double v) { return Math.round(v * 10) / 10.0; }
}
