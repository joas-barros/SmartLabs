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
}
