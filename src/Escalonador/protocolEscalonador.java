package Escalonador;

import Processos.ProcessosProtocol;
import java.util.List;

public interface protocolEscalonador {
    String nome();
    void executar(List<? extends ProcessosProtocol> processos);
}
