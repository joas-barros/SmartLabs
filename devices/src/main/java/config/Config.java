package config;

public class Config {

    private Config() {
        // Construtor privado para evitar instância
    }

    public static final String BROKER_IP = System.getenv("BROKER_IP") != null ? System.getenv("BROKER_IP") : "localhost";
    public static final String GATEWAY_IP = System.getenv("GATEWAY_IP") != null ? System.getenv("GATEWAY_IP") : "localhost";

    public static final String BROKER_MQTT_URL = "tcp://" + BROKER_IP + ":1883";
    public static final String GATEWAY_COAP_URL = "coap://" + GATEWAY_IP + ":5683";

    public static final int PCS_PER_LAB = 10;
    public static final int PC_INTERVAL_SEC = 3;
    public static final int AIR_CONDITIONING_INTERVAL_SEC = 5;
    public static final int PROJECTOR_INTERVAL_SEC = 5;

    public static final String LAB1 = "LAB1"; // MQTT
    public static final String LAB2 = "LAB2"; // CoAP

    // MQTT topics
    public static String pcTopic(String lab, String pcId) {
        return "lab/" + lab.replace("LAB", "") + "/pc/" + pcId + "/data";
    }

    public static String acTopic(String lab) {
        return "lab/" + lab.replace("LAB", "") + "/ac/data";
    }

    public static String projectorTopic(String lab) {
        return "lab/" + lab.replace("LAB", "") + "/projector/data";
    }

    public static String alertTopic(String lab) {
        return "lab/" + lab.replace("LAB", "") + "/alerts";
    }

    // CoAP resource paths
    public static String pcCoapResource(String pcId) {
        return "/lab/pc/" + pcId;
    }

    public static String acCoapResource() {
        return "/lab/ac";
    }

    public static String projectorCoapResource() {
        return "/lab/projector";
    }

    public static String alertCoapResource() {
        return "/lab/alerts";
    }
}
