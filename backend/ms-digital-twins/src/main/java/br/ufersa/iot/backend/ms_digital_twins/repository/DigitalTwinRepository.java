package br.ufersa.iot.backend.ms_digital_twins.repository;

import br.ufersa.iot.backend.ms_digital_twins.model.DeviceTwin;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class DigitalTwinRepository {

    private final Map<String, DeviceTwin> storage = new ConcurrentHashMap<>();

    /**
     * Guarda ou atualiza um DeviceTwin.
     * Retorna um Mono contendo o twin guardado.
     */
    public Mono<DeviceTwin> save(DeviceTwin twin) {
        String key = buildKey(twin.getLab(), twin.getDeviceId());
        storage.put(key, twin);
        return Mono.just(twin);
    }

    /**
     * Procura um dispositivo específico pelo seu Laboratório e ID.
     */
    public Mono<DeviceTwin> findById(String lab, String deviceId) {
        DeviceTwin twin = storage.get(buildKey(lab, deviceId));
        // Se não encontrar, retorna um Mono vazio (Mono.empty()),
        // o que é o padrão reativo para "Not Found" em vez de retornar null.
        return twin != null ? Mono.just(twin) : Mono.empty();
    }

    /**
     * Retorna todos os dispositivos de todos os laboratórios (útil para um dashboard global).
     */
    public Flux<DeviceTwin> findAll() {
        return Flux.fromIterable(storage.values());
    }

    /**
     * Filtra os dispositivos para retornar apenas os de um laboratório específico.
     */
    public Flux<DeviceTwin> findByLab(String lab) {
        return Flux.fromIterable(storage.values())
                .filter(twin -> twin.getLab().equalsIgnoreCase(lab));
    }

    /**
     * Método utilitário para garantir que a chave seja sempre consistente.
     */
    private String buildKey(String lab, String deviceId) {
        return lab.toUpperCase() + ":" + deviceId.toUpperCase();
    }
}
