package br.ufersa.iot.backend.ms_digital_twins.config;

public final class RabbitMqConfig {
    private RabbitMqConfig() {
    }

    // Suporta rodar localmente ou no Docker Compose
    public static final String HOST = System.getenv("RABBITMQ_HOST") != null ? System.getenv("RABBITMQ_HOST") : "localhost";
    public static final int PORT = 5672;
    public static final String USER = "guest";
    public static final String PASS = "guest";

    public static final String EXCHANGE_EVENTOS = "lab.events";

    /** Este microsserviço precisa de TODOS os eventos para manter os twins atualizados. */
    public static final String FILA_DIGITAL_TWINS = "digital-twins.todos-eventos";

    /** Routing key wildcard: recebe status, alertas, energia e temperatura. */
    public static final String ROUTING_KEY_WILDCARD = "#";
}
