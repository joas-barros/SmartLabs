package coap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

public class CoapPublisher {
    private final String baseUri;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    public CoapPublisher(String baseUri) {
        this.baseUri = baseUri;
    }

    public void publish(String resource, Object payload) {
        CoapClient client = new CoapClient(baseUri + resource);
        try {
            byte[] json = objectMapper.writeValueAsBytes(payload);
            client.useNONs(); // mensagens não confirmáveis (NON), análogo ao QoS 0 do MQTT

            CoapResponse response = client.post(json, MediaTypeRegistry.APPLICATION_JSON);

            if (response == null || response.getCode() != CoAP.ResponseCode.CHANGED
                    && response.getCode() != CoAP.ResponseCode.CREATED) {
                System.err.printf("[CoAP] Resposta inesperada de %s: %s%n",
                        resource, response != null ? response.getCode() : "sem resposta");
            } else {
                System.out.printf("[CoAP] Publicado payload %s com sucesso em %s%n", payload, resource);
            }
        } catch (Exception e) {
            System.err.printf("[CoAP] Falha ao publicar em %s: %s%n", resource, e.getMessage());
        } finally {
            client.shutdown();
        }
    }

    public void publishAlert(String resource, Object payload) {
        CoapClient client = new CoapClient(baseUri + resource);
        try {
            byte[] json = objectMapper.writeValueAsBytes(payload);
            CoapResponse response = client.post(json, MediaTypeRegistry.APPLICATION_JSON);

            if (response == null) {
                System.err.printf("[CoAP] Alerta sem resposta do gateway: %s%n", resource);
            }
        } catch (Exception e) {
            System.err.printf("[CoAP] Falha ao publicar alerta em %s: %s%n", resource, e.getMessage());
        } finally {
            client.shutdown();
        }
    }
}
