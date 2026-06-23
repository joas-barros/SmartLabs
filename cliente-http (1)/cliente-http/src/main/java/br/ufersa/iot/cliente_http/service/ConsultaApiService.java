package br.ufersa.iot.cliente_http.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class ConsultaApiService {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ConsultaApiService(RestTemplate restTemplate,
                              @Value("${api-gateway.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    // ─── ms-digital-twins (Através do Gateway) ──────────────────────────

    /** GET /api/twins/{lab} */
    public String statusLaboratorio(String lab) {
        return get("/api/twins/" + lab);
    }

    /** GET /api/twins/{lab}/{id} */
    public String digitalTwin(String lab, String id) {
        return get("/api/twins/" + lab + "/" + id);
    }

    /** GET /api/twins */
    public String listarTwins() {
        return get("/api/twins");
    }

    // ─── ms-laboratorio (Através do Gateway) ────────────────────────────

    /** GET /labs/{lab}/historico?intervalo={intervalo} */
    public String historicoLaboratorio(String lab, String intervalo) {
        return get("/labs/" + lab + "/historico?intervalo=" + intervalo);
    }

    /** GET /labs/{lab}/historico/{dispositivoId}?intervalo={intervalo} */
    public String historicoDispositivo(String lab, String dispositivoId, String intervalo) {
        return get("/labs/" + lab + "/historico/" + dispositivoId + "?intervalo=" + intervalo);
    }

    /** GET /labs/{lab}/estatisticas?intervalo={intervalo} */
    public String estatisticasLaboratorio(String lab, String intervalo) {
        return get("/labs/" + lab + "/estatisticas?intervalo=" + intervalo);
    }

    /** GET /labs/{lab}/processamento */
    public String processamentoLaboratorio(String lab) {
        return get("/labs/" + lab + "/processamento");
    }

    // ─── Agregado (Através do Gateway) ──────────────────────────────────

    /** GET /labs/{lab}/painel */
    public String painelLaboratorio(String lab) {
        return get("/labs/" + lab + "/painel");
    }

    // ─── Auxiliar ───────────────────────────────────────────────────────

    private String get(String caminho) {
        try {
            return restTemplate.getForObject(baseUrl + caminho, String.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ConsultaException("Recurso não encontrado (404): " + caminho);
        } catch (HttpServerErrorException.BadGateway e) {
            throw new ConsultaException("Serviço de backend indisponível (502). O Gateway está de pé, mas os microsserviços não responderam.");
        } catch (ResourceAccessException e) {
            throw new ConsultaException("Não foi possível conectar à API Gateway em "
                    + baseUrl + " (" + e.getMessage() + ")");
        } catch (Exception e) {
            throw new ConsultaException("Erro ao consultar " + caminho + ": " + e.getMessage());
        }
    }

    public static class ConsultaException extends RuntimeException {
        public ConsultaException(String message) { super(message); }
    }
}