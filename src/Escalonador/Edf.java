package Escalonador;

import java.util.*;
import Processos.ProcessosProtocol;
import Processos.Estados;

public class Edf implements protocolEscalonador {
    public String nome() { return "EDF (tempo real)"; }

    public void executar(List<? extends ProcessosProtocol> lista) {
        var todos = new ArrayList<ProcessosProtocol>(lista);
        int relogio = 0, concluidos = 0;

        while (concluidos < todos.size()) {
            final int agora = relogio;
            ProcessosProtocol atual = todos.stream()
                .filter(p -> !p.terminou() && p.getChegada() <= agora)
                .min(Comparator.comparingInt(ProcessosProtocol::getDeadline)
                               .thenComparingInt(ProcessosProtocol::getChegada))
                .orElse(null);

            if (atual == null) { relogio++; continue; }   // CPU ociosa

            atual.setEstado(Estados.EXECUTANDO);
            atual.registrarInicio(relogio);
            atual.executar(1);              // reavalia a cada unidade (preempção)
            relogio++;

            if (atual.terminou()) {
                atual.registrarConclusao(relogio);
                concluidos++;
            } else {
                atual.setEstado(Estados.PRONTO);
            }
        }
    }
}
