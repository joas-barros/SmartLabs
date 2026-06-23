package br.ufersa.iot.api_gateway.router;

import br.ufersa.iot.api_gateway.handler.GatewayHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class GatewayRouter {
    @Bean
    public RouterFunction<ServerResponse> rotasGateway(GatewayHandler handler) {
        return RouterFunctions.route()
                // ─── Rotas expostas para o ms-digital-twins ───
                .GET("/api/twins", handler::todosTwins)
                .GET("/api/twins/{lab}", handler::twinsPorLab)
                .GET("/api/twins/{lab}/{id}", handler::twinPorId)

                // ─── Rotas expostas para o ms-laboratorio ───
                .GET("/labs/{lab}/historico", handler::historicoLaboratorio)
                .GET("/labs/{lab}/historico/{dispositivoId}", handler::historicoDispositivo)
                .GET("/labs/{lab}/estatisticas", handler::estatisticasLaboratorio)
                .GET("/labs/{lab}/processamento", handler::processamentoLaboratorio)

                // ─── Rota agregada ───
                .GET("/labs/{lab}/painel", handler::painelLaboratorio)
                .build();
    }
}
