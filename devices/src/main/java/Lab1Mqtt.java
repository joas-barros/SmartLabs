import mqtt.MqttOrchestrator;

public class Lab1Mqtt {

    public static void main(String[] args) {
        config.LoggerConfig.setup("devices-lab1-mqtt.log");
        MqttOrchestrator.run();
    }
}

