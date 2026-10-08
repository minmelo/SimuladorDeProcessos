package Escalonador;

import java.util.*;
import Processos.ProcessosProtocol;

public class RoundRobin implements protocolEscalonador {
    private final int quantum;
    private final Deque<ProcessosProtocol> prontos = new ArrayDeque<>();
    private ProcessosProtocol atual;
    private int usado;

    public RoundRobin(int quantum) {
        if (quantum <= 0) throw new IllegalArgumentException("Quantum deve ser > 0");
        this.quantum = quantum;
    }

    public String nome() { return "Round Robin (q=" + quantum + ")"; }

    public void admitir(ProcessosProtocol p) { prontos.addLast(p); }

    public ProcessosProtocol escolher() {
        if (atual == null) {
            atual = prontos.pollFirst();
            usado = 0;
        }
        return atual;
    }

    public void aposTick(ProcessosProtocol p) {
        usado++;
        if (p.terminou()) {
            atual = null;
        } else if (usado >= quantum) {      // quantum esgotado: vai para o fim da fila
            prontos.addLast(atual);
            atual = null;
        }
    }

    public void remover(int id) {
        prontos.removeIf(x -> x.getId() == id);
        if (atual != null && atual.getId() == id) atual = null;
    }
}
