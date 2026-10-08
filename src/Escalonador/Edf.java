package Escalonador;

import java.util.*;
import Processos.ProcessosProtocol;

public class Edf implements protocolEscalonador {
    private final List<ProcessosProtocol> prontos = new ArrayList<>();

    public String nome() { return "EDF (tempo real)"; }

    public void admitir(ProcessosProtocol p) { prontos.add(p); }

    public ProcessosProtocol escolher() {
        return prontos.stream()
            .min(Comparator.comparingInt(ProcessosProtocol::getDeadline)
                           .thenComparingInt(ProcessosProtocol::getChegada))
            .orElse(null);
    }

    public void aposTick(ProcessosProtocol p) {
        if (p.terminou()) prontos.remove(p);
    }

    public void remover(int id) { prontos.removeIf(x -> x.getId() == id); }
}
