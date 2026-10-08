package Escalonador;

import java.util.*;
import Processos.ProcessosProtocol;

public class Fcfs implements protocolEscalonador {
    private final Deque<ProcessosProtocol> fila = new ArrayDeque<>();
    private ProcessosProtocol atual;

    public String nome() { return "FCFS"; }

    public void admitir(ProcessosProtocol p) { fila.addLast(p); }

    public ProcessosProtocol escolher() {
        if (atual == null) atual = fila.pollFirst();
        return atual;
    }

    public void aposTick(ProcessosProtocol p) {
        if (p.terminou()) atual = null;
    }

    public void remover(int id) {
        fila.removeIf(x -> x.getId() == id);
        if (atual != null && atual.getId() == id) atual = null;
    }
}
