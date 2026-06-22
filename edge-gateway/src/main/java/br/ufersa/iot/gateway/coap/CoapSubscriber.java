package br.ufersa.iot.gateway.coap;

import br.ufersa.iot.gateway.analysis.DataAnalyzer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import config.Config;
import model.AirConditioningDTO;
import model.PCDTO;
import model.ProjectorDTO;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.CoapExchange;

public class CoapSubscriber implements AutoCloseable {

    private final CoapServer server;
    private final DataAnalyzer dataAnalyzer;
    private final ObjectMapper objectMapper;

    public CoapSubscriber(DataAnalyzer dataAnalyzer) {
        this.dataAnalyzer = dataAnalyzer;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        // Instancia o servidor CoAP na porta padrão (5683) com a configuração padrão
        org.eclipse.californium.elements.config.Configuration config = org.eclipse.californium.elements.config.Configuration.createStandardWithoutFile();
        this.server = new CoapServer(config, 5683);

        // Constrói a estrutura hierárquica de recursos CoAP
        // CoAP URL: coap://localhost:5683/lab/...
        CoapResource lab = new CoapResource("lab");

        CoapResource pcParent = new CoapResource("pc");
        for (int i = 1; i <= Config.PCS_PER_LAB; i++) {
            String pcId = "PC" + String.format("%02d", i);
            pcParent.add(new PCResource(pcId));
        }

        lab.add(pcParent);
        lab.add(new ACResource());
        lab.add(new ProjectorResource());
        lab.add(new AlertsResource());

        server.add(lab);

        System.out.println("[CoAP-RX] Inicializando Servidor CoAP...");
        server.start();
        System.out.println("[CoAP-RX] Servidor CoAP rodando com sucesso na porta 5683.");
    }

    @Override
    public void close() {
        server.stop();
        server.destroy();
        System.out.println("[CoAP-RX] Servidor CoAP finalizado.");
    }

    // ==========================================
    // RECURSOS COAP INDIVIDUAIS (ENDPOINTS)
    // ==========================================

    private class PCResource extends CoapResource {
        public PCResource(String pcId) {
            super(pcId);
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
            System.out.printf("[CoAP-RX-DEBUG] Recebida requisicao POST no PC: %s%n", getName());
            try {
                byte[] payload = exchange.getRequestPayload();
                PCDTO pc = objectMapper.readValue(payload, PCDTO.class);
                dataAnalyzer.processPC(pc);
                exchange.respond(ResponseCode.CHANGED);
            } catch (Exception e) {
                System.err.printf("[CoAP-RX] Erro ao processar telemetria do PC %s: %s%n", getName(), e.getMessage());
                exchange.respond(ResponseCode.BAD_REQUEST, e.getMessage());
            }
        }
    }

    private class ACResource extends CoapResource {
        public ACResource() {
            super("ac");
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
            System.out.println("[CoAP-RX-DEBUG] Recebida requisicao POST no AC");
            try {
                byte[] payload = exchange.getRequestPayload();
                AirConditioningDTO ac = objectMapper.readValue(payload, AirConditioningDTO.class);
                dataAnalyzer.processAC(ac);
                exchange.respond(ResponseCode.CHANGED);
            } catch (Exception e) {
                System.err.printf("[CoAP-RX] Erro ao processar telemetria de AC: %s%n", e.getMessage());
                exchange.respond(ResponseCode.BAD_REQUEST, e.getMessage());
            }
        }
    }

    private class ProjectorResource extends CoapResource {
        public ProjectorResource() {
            super("projector");
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
            System.out.println("[CoAP-RX-DEBUG] Recebida requisicao POST no Projetor");
            try {
                byte[] payload = exchange.getRequestPayload();
                ProjectorDTO proj = objectMapper.readValue(payload, ProjectorDTO.class);
                dataAnalyzer.processProjector(proj);
                exchange.respond(ResponseCode.CHANGED);
            } catch (Exception e) {
                System.err.printf("[CoAP-RX] Erro ao processar telemetria de Projetor: %s%n", e.getMessage());
                exchange.respond(ResponseCode.BAD_REQUEST, e.getMessage());
            }
        }
    }

    private class AlertsResource extends CoapResource {
        public AlertsResource() {
            super("alerts");
        }

        @Override
        public void handlePOST(CoapExchange exchange) {
            System.out.println("[CoAP-RX-DEBUG] Recebida requisicao POST em Alertas");
            try {
                byte[] payload = exchange.getRequestPayload();
                JsonNode json = objectMapper.readTree(payload);
                String labName = json.has("lab") ? json.get("lab").asText() : "Desconhecido";
                String dispositivo = json.has("dispositivo") ? json.get("dispositivo").asText() : "Desconhecido";
                String alerta = json.has("alerta") ? json.get("alerta").asText() : "Desconhecido";

                dataAnalyzer.processExternalAlert(labName, dispositivo, alerta);
                exchange.respond(ResponseCode.CREATED);
            } catch (Exception e) {
                System.err.printf("[CoAP-RX] Erro ao processar alerta externo via CoAP: %s%n", e.getMessage());
                exchange.respond(ResponseCode.BAD_REQUEST, e.getMessage());
            }
        }
    }
}
