package br.ufersa.iot.api_gateway.handler;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Configuration
public class GatewayHandler {

    private final WebClient digitalTwinsClient;
    private final WebClient laboratorioClient;

    public GatewayHandler(WebClient digitalTwinsClient, WebClient laboratorioClient) {
        this.digitalTwinsClient = digitalTwinsClient;
        this.laboratorioClient  = laboratorioClient;
    }

    // ─── ms-digital-twins ────────────────────────────────────────────────

    public Mono<ServerResponse> todosTwins(ServerRequest req) {
        // Redireciona para o endpoint raiz do ms-digital-twins
        return proxy(digitalTwinsClient, "/api/twins");
    }

    public Mono<ServerResponse> twinsPorLab(ServerRequest req) {
        String lab = req.pathVariable("lab");
        // Redireciona repassando o laboratório
        return proxy(digitalTwinsClient, "/api/twins/" + lab);
    }

    public Mono<ServerResponse> twinPorId(ServerRequest req) {
        String lab = req.pathVariable("lab");
        String id = req.pathVariable("id");
        // Redireciona repassando laboratório e o dispositivo
        return proxy(digitalTwinsClient, "/api/twins/" + lab + "/" + id);
    }

    // ─── ms-laboratorio ──────────────────────────────────────────────────

    public Mono<ServerResponse> historicoLaboratorio(ServerRequest req) {
        String lab       = req.pathVariable("lab");
        String intervalo = req.queryParam("intervalo").orElse("24h");
        return proxy(laboratorioClient, "/labs/" + lab + "/historico?intervalo=" + intervalo);
    }

    public Mono<ServerResponse> historicoDispositivo(ServerRequest req) {
        String lab          = req.pathVariable("lab");
        String dispositivoId = req.pathVariable("dispositivoId");
        String intervalo    = req.queryParam("intervalo").orElse("24h");
        return proxy(laboratorioClient,
                "/labs/" + lab + "/historico/" + dispositivoId + "?intervalo=" + intervalo);
    }

    public Mono<ServerResponse> estatisticasLaboratorio(ServerRequest req) {
        String lab       = req.pathVariable("lab");
        String intervalo = req.queryParam("intervalo").orElse("24h");
        return proxy(laboratorioClient, "/labs/" + lab + "/estatisticas?intervalo=" + intervalo);
    }

    public Mono<ServerResponse> processamentoLaboratorio(ServerRequest req) {
        return proxy(laboratorioClient, "/labs/" + req.pathVariable("lab") + "/processamento");
    }

    // ─── Endpoint agregado (Mono.zip) ────────────────────────────────────

    /**
     * Painel consolidado: consulta ms-digital-twins e ms-laboratorio em
     * paralelo e combina os resultados com {@code Mono.zip}.
     */
    public Mono<ServerResponse> painelLaboratorio(ServerRequest req) {
        String lab = req.pathVariable("lab").toUpperCase();

        // 1. Busca o status atual no ms-digital-twins
        // Usamos Object.class pois a resposta é uma Lista (Array JSON)
        Mono<Object> statusMono = digitalTwinsClient.get()
                .uri("/api/twins/" + lab)
                .retrieve()
                .bodyToMono(Object.class)
                .onErrorReturn(List.of(Map.of("erro", "ms-digital-twins indisponível")));

        // 2. Busca as estatísticas no ms-laboratorio
        Mono<Object> estatisticasMono = laboratorioClient.get()
                .uri("/labs/" + lab + "/estatisticas?intervalo=1h")
                .retrieve()
                .bodyToMono(Object.class)
                .onErrorReturn(Map.of("erro", "ms-laboratorio indisponível"));

        // 3. Aguarda os dois terminarem e agrupa a resposta
        return Mono.zip(statusMono, estatisticasMono)
                .flatMap(tupla -> {
                    Map<String, Object> resposta = Map.of(
                            "laboratorio",            lab,
                            "statusAtual",            tupla.getT1(),
                            "estatisticasUltimaHora", tupla.getT2()
                    );
                    return ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(resposta);
                });
    }

    // ─── Auxiliar ────────────────────────────────────────────────────────

    private Mono<ServerResponse> proxy(WebClient client, String uri) {
        return client.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(Object.class)
                .flatMap(corpo -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(corpo))
                .onErrorResume(erro -> ServerResponse.status(502)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of(
                                "erro",    "Serviço indisponível",
                                "detalhe", erro.getMessage())));
    }
}
