package coap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CoapPublisher {
    private final String baseUri;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private final Map<String, CoapClient> clientCache = new ConcurrentHashMap<>();

    public CoapPublisher(String baseUri) {
        this.baseUri = baseUri;
    }

    private CoapClient getClient(String resource) {
        // Se o cliente para este recurso não existe, cria. Se existe, reaproveita.
        return clientCache.computeIfAbsent(resource, r -> {
            CoapClient client = new CoapClient(baseUri + r);

            // CRÍTICO: Timeout de 2 segundos (2000 ms). Impede que as threads
            // congelem por 4 minutos se o Gateway estiver offline ou iniciando!
            client.setTimeout(2000L);

            return client;
        });
    }

    public void publish(String resource, Object payload) {
        try {
            CoapClient client = getClient(resource);
            byte[] json = objectMapper.writeValueAsBytes(payload);
            client.useNONs(); // mensagens não confirmáveis (NON), análogo ao QoS 0 do MQTT

            CoapResponse response = client.post(json, MediaTypeRegistry.APPLICATION_JSON);

            if (response == null || response.getCode() != CoAP.ResponseCode.CHANGED
                    && response.getCode() != CoAP.ResponseCode.CREATED) {
                System.err.printf("[CoAP] Resposta inesperada de %s: %s%n",
                        resource, response != null ? response.getCode() : "sem resposta");
            } else {
                System.out.printf("[CoAP] Payload publicado com sucesso em %s%n", resource);
            }
        } catch (Exception e) {
            System.err.printf("[CoAP] Falha ao publicar em %s: %s%n", resource, e.getMessage());
        }
    }

    public void publishAlert(String resource, Object payload) {
        try {
            CoapClient client = getClient(resource);
            byte[] json = objectMapper.writeValueAsBytes(payload);
            CoapResponse response = client.post(json, MediaTypeRegistry.APPLICATION_JSON);

            if (response == null) {
                System.err.printf("[CoAP] Alerta sem resposta do gateway: %s%n", resource);
            } else {
                System.out.printf("[CoAP] Alerta publicado com sucesso em %s%n", resource);
            }
        } catch (Exception e) {
            System.err.printf("[CoAP] Falha ao publicar alerta em %s: %s%n", resource, e.getMessage());
        }
    }
}