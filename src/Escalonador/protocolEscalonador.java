package Escalonador;

import Processos.ProcessosProtocol;
import java.util.List;

public interface protocolEscalonador {
    String nome();
    void admitir(ProcessosProtocol p);          
    ProcessosProtocol escolher();// quem roda neste tick (null = CPU ociosa)
    void aposTick(ProcessosProtocol p);// atualiza o estado interno após o tick
    void remover(int id);
}
