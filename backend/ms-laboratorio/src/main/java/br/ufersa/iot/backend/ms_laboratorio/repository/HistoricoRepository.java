package br.ufersa.iot.backend.ms_laboratorio.repository;

import br.ufersa.iot.backend.ms_laboratorio.model.RegistroHistorico;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@Repository
public class HistoricoRepository {

    private static final int CAPACIDADE_POR_LAB = 5000;

    private final ConcurrentHashMap<String, LinkedBlockingQueue<RegistroHistorico>> historico =
            new ConcurrentHashMap<>();

    public void adicionar(RegistroHistorico registro) {
        LinkedBlockingQueue<RegistroHistorico> fila = historico.computeIfAbsent(
                registro.getLaboratorio(),
                k -> new LinkedBlockingQueue<>(CAPACIDADE_POR_LAB));

        if (!fila.offer(registro)) {
            fila.poll();
            fila.offer(registro);
        }
    }

    public List<RegistroHistorico> buscarDesde(String laboratorio, Instant desde) {
        LinkedBlockingQueue<RegistroHistorico> fila = historico.get(laboratorio.toUpperCase());
        if (fila == null) return List.of();

        List<RegistroHistorico> resultado = new ArrayList<>();
        for (RegistroHistorico r : fila) {
            if (r.getTimestamp().isAfter(desde)) resultado.add(r);
        }
        return resultado;
    }

    public List<RegistroHistorico> buscarPorDispositivo(String laboratorio, String dispositivoId, Instant desde) {
        return buscarDesde(laboratorio, desde).stream()
                .filter(r -> dispositivoId.equals(r.getDispositivoId()))
                .toList();
    }

    public int totalRegistros(String laboratorio) {
        LinkedBlockingQueue<RegistroHistorico> fila = historico.get(laboratorio.toUpperCase());
        return fila != null ? fila.size() : 0;
    }
}
