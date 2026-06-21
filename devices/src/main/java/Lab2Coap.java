import coap.CoapOrchestrator;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.UdpConfig;

public class Lab2Coap {
    public static void main(String[] args) {
        config.LoggerConfig.setup("devices-lab2-coap.log");
        CoapConfig.register();
        UdpConfig.register();
        CoapOrchestrator.run();
    }
}
