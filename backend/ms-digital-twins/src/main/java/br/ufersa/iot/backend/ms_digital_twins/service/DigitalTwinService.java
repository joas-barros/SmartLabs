package br.ufersa.iot.backend.ms_digital_twins.service;

import br.ufersa.iot.backend.ms_digital_twins.model.DeviceTwin;
import br.ufersa.iot.backend.ms_digital_twins.repository.DigitalTwinRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class DigitalTwinService {
    private final DigitalTwinRepository repository;

    public DigitalTwinService(DigitalTwinRepository repository) {
        this.repository = repository;
    }

    public Mono<DeviceTwin> processTelemetry(String lab, String deviceId, String deviceType, Map<String, Object> state) {
        return repository.updateState(lab, deviceId, deviceType, state);
    }

    public Mono<DeviceTwin> processAlert(String lab, String deviceId, String description) {
        return repository.addEvent(lab, deviceId, description);
    }

    public Mono<DeviceTwin> updateConnectivity(String lab, String deviceId, boolean isOnline) {
        return repository.updateOnlineStatus(lab, deviceId, isOnline);
    }

    public Flux<DeviceTwin> getAllTwins() {
        return repository.findAll();
    }

    public Flux<DeviceTwin> getTwinsByLab(String lab) {
        return repository.findByLab(lab);
    }

    public Mono<DeviceTwin> getTwinById(String lab, String deviceId) {
        return repository.findById(lab, deviceId);
    }
}
