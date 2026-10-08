//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import java.util.*;
import java.util.function.Supplier;
import Escalonador.*;
import Processos.*;

List<Processo> criar() {
    return List.of(
        new Processo(1, "P1", 0, 5, 12),
        new Processo(2, "P2", 2, 3, 8),
        new Processo(3, "P3", 4, 1, 6),
        new Processo(4, "P4", 5, 2, 10),
        new Processo(5, "P5", 7, 4, 20)
    );
}

void rodar(protocolEscalonador alg) {
    var procs = criar();               // cópia nova para cada algoritmo
    alg.executar(procs);

    IO.println("\n=== " + alg.nome() + " ===");
    double somaE = 0, somaR = 0;
    for (var p : procs) {
        boolean perdeu = p.getConclusao() > p.getDeadline();
        IO.println(p.getNome() + " inicio=" + p.getInicio()
            + " fim=" + p.getConclusao()
            + " espera=" + p.getEspera()
            + " retorno=" + p.getRetorno()
            + (perdeu ? "  [PERDEU DEADLINE]" : ""));
        somaE += p.getEspera();
        somaR += p.getRetorno();
    }
    IO.println("Espera média: " + somaE / procs.size());
    IO.println("Retorno médio: " + somaR / procs.size());
}

void main() {
    rodar(new Fcfs());
    rodar(new RoundRobin(2));
    rodar(new Edf());
}
