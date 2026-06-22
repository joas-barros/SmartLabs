package br.ufersa.iot.backend.ms_laboratorio.repository;

import br.ufersa.iot.backend.ms_laboratorio.model.EstadoProcessado;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositório em memória do estado de processamento contínuo por laboratório.
 * Cada laboratório tem exatamente um Estado ativo.
 */
@Repository
public class EstadoProcessadoRepository {

    private final ConcurrentHashMap<String, EstadoProcessado> estados = new ConcurrentHashMap<>();

    public EstadoProcessado obterOuCriar(String laboratorio) {
        return estados.computeIfAbsent(laboratorio.toUpperCase(), EstadoProcessado::new);
    }

    public Optional<EstadoProcessado> buscar(String laboratorio) {
        return Optional.ofNullable(estados.get(laboratorio.toUpperCase()));
    }

    public Collection<EstadoProcessado> listarTodos() {
        return estados.values();
    }
}
