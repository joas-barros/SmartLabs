package br.ufersa.iot.backend.ms_laboratorio.config;

public final class RabbitMqConfig {

    private RabbitMqConfig() {
    }

    public static final String HOST = System.getenv("RABBITMQ_HOST") != null ? System.getenv("RABBITMQ_HOST") : "localhost";
    public static final int PORT = 5672;
    public static final String USER = "guest";
    public static final String PASS = "guest";

    public static final String EXCHANGE_EVENTOS = "lab.events";
    public static final String FILA_LABORATORIO = "laboratorio.processamento";
    public static final String ROUTING_KEY_WILDCARD = "#";

    // Adicionado as Routing Keys do Gateway para sabermos classificar o evento
    public static final String RK_TELEMETRIA_PC = "telemetria.pc";
    public static final String RK_TELEMETRIA_AC = "telemetria.ac";
    public static final String RK_TELEMETRIA_PROJETOR = "telemetria.projetor";
    public static final String RK_METRICA_AGREGADA = "metrica.agregada";
}
