package br.ufersa.iot.gateway.config;

public final class RabbitmqConfig {
    private RabbitmqConfig() {
    }

    // ─── RabbitMQ ───
    public static final String RABBITMQ_HOST = System.getenv("RABBITMQ_HOST") != null ? System.getenv("RABBITMQ_HOST") : "localhost";
    public static final int RABBITMQ_PORT = 5672;
    public static final String RABBITMQ_USER = "guest";
    public static final String RABBITMQ_PASS = "guest";

    /** Exchange tipo "topic" usada para rotear eventos para as filas corretas. */
    public static final String EXCHANGE_EVENTOS = "lab.events";

    // ─── Filas ───
    public static final String FILA_STATUS_DISPOSITIVOS = "status_dispositivos";
    public static final String FILA_ALERTAS_DESEMPENHO = "alertas_desempenho";
    public static final String FILA_TEMPERATURA_AMBIENTE = "temperatura_ambiente";
    public static final String FILA_EVENTOS_ENERGIA = "eventos_energia";

    // ─── Routing keys ───
    public static final String RK_TELEMETRIA_PC = "telemetria.pc";
    public static final String RK_TELEMETRIA_AC = "telemetria.ac";
    public static final String RK_TELEMETRIA_PROJETOR = "telemetria.projetor";
    public static final String RK_ALERTA_CRITICO = "alerta.critico";
    public static final String RK_ALERTA_SEGURANCA = "alerta.seguranca";
    public static final String RK_METRICA_AGREGADA = "metrica.agregada";

    // ─── Agregação local ───
    /** Intervalo, em segundos, para cálculo e envio das métricas agregadas. */
    public static final int INTERVALO_AGREGACAO_SEGUNDOS = 5;

    // ─── Modo offline ───
    /** Capacidade máxima da fila local enquanto o RabbitMQ está inacessível. */
    public static final int CAPACIDADE_FILA_OFFLINE = 5000;

    /** Intervalo, em segundos, para tentar reenviar a fila offline ao RabbitMQ. */
    public static final int INTERVALO_RETRY_OFFLINE_SEGUNDOS = 5;
}
