package br.ufersa.iot.backend.ms_digital_twins.router;

import br.ufersa.iot.backend.ms_digital_twins.handler.DigitalTwinHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class DigitalTwinRouter {
    @Bean
    public RouterFunction<ServerResponse> twinRoutes(DigitalTwinHandler handler) {
        // Agrupamos todas as rotas debaixo do prefixo "/api/twins"
        return RouterFunctions.route()
                .path("/api/twins", builder -> builder
                        .GET("", handler::getAll)                  // GET /api/twins
                        .GET("/{lab}", handler::getByLab)          // GET /api/twins/LAB1
                        .GET("/{lab}/{id}", handler::getById)      // GET /api/twins/LAB1/PC01
                )
                .build();
    }
}
