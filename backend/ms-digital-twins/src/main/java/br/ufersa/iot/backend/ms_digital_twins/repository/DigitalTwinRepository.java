package br.ufersa.iot.backend.ms_digital_twins.repository;

import br.ufersa.iot.backend.ms_digital_twins.model.DeviceTwin;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class DigitalTwinRepository {

    private final Map<String, DeviceTwin> storage = new ConcurrentHashMap<>();

    public Mono<DeviceTwin> updateState(String lab, String deviceId, String deviceType, Map<String, Object> newState) {
        String key = buildKey(lab, deviceId);

        // O compute garante que a leitura e a escrita ocorram num único bloco sem interferência
        DeviceTwin updatedTwin = storage.compute(key, (k, existingTwin) -> {
            if (existingTwin == null) {
                DeviceTwin newTwin = new DeviceTwin(lab, deviceId, deviceType);
                newTwin.setState(newState);
                return newTwin;
            } else {
                existingTwin.setState(newState);
                existingTwin.setLastUpdate(Instant.now());
                existingTwin.setOnline(true);
                return existingTwin;
            }
        });

        return Mono.just(updatedTwin);
    }

    /**
     * Adiciona um evento ao histórico de forma atômica (apenas se o dispositivo existir).
     */
    public Mono<DeviceTwin> addEvent(String lab, String deviceId, String descricao) {
        String key = buildKey(lab, deviceId);

        DeviceTwin updatedTwin = storage.computeIfPresent(key, (k, existingTwin) -> {
            existingTwin.addEvento(descricao);
            return existingTwin;
        });

        return updatedTwin != null ? Mono.just(updatedTwin) : Mono.empty();
    }

    /**
     * Atualiza o status de conectividade (Online/Offline)
     */
    public Mono<DeviceTwin> updateOnlineStatus(String lab, String deviceId, boolean isOnline) {
        String key = buildKey(lab, deviceId);

        DeviceTwin updatedTwin = storage.computeIfPresent(key, (k, existingTwin) -> {
            existingTwin.setOnline(isOnline);
            existingTwin.setLastUpdate(Instant.now());
            return existingTwin;
        });

        return updatedTwin != null ? Mono.just(updatedTwin) : Mono.empty();
    }

    // ==========================================
    // MÉTODOS DE CONSULTA (HTTP GETs)
    // ==========================================

    public Mono<DeviceTwin> findById(String lab, String deviceId) {
        DeviceTwin twin = storage.get(buildKey(lab, deviceId));
        return twin != null ? Mono.just(twin) : Mono.empty();
    }

    public Flux<DeviceTwin> findAll() {
        return Flux.fromIterable(storage.values());
    }

    public Flux<DeviceTwin> findByLab(String lab) {
        return Flux.fromIterable(storage.values())
                .filter(twin -> twin.getLab().equalsIgnoreCase(lab));
    }

    private String buildKey(String lab, String deviceId) {
        return lab.toUpperCase() + ":" + deviceId.toUpperCase();
    }
}
