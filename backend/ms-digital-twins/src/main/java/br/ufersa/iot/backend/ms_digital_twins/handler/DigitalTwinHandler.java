package br.ufersa.iot.backend.ms_digital_twins.handler;

import br.ufersa.iot.backend.ms_digital_twins.model.DeviceTwin;
import br.ufersa.iot.backend.ms_digital_twins.service.DigitalTwinService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class DigitalTwinHandler {
    private final DigitalTwinService service;

    public DigitalTwinHandler(DigitalTwinService service) {
        this.service = service;
    }

    /**
     * Retorna todos os Digital Twins armazenados em memória.
     */
    public Mono<ServerResponse> getAll(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.getAllTwins(), DeviceTwin.class);
    }

    /**
     * Retorna os Digital Twins filtrados por um Laboratório específico.
     */
    public Mono<ServerResponse> getByLab(ServerRequest request) {
        String lab = request.pathVariable("lab");

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.getTwinsByLab(lab), DeviceTwin.class);
    }

    /**
     * Retorna um único Digital Twin (ex: LAB1 e PC01).
     * Se não existir, retorna um HTTP 404 (Not Found).
     */
    public Mono<ServerResponse> getById(ServerRequest request) {
        String lab = request.pathVariable("lab");
        String id = request.pathVariable("id");

        return service.getTwinById(lab, id)
                .flatMap(twin -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(twin))
                .switchIfEmpty(ServerResponse.notFound().build());
    }
}
