package Sistema;

import java.util.List;

/** Cenários de teste fixos para comparar os algoritmos de escalonamento. */
public final class Cenarios {
    private Cenarios() {}

    /** ioApos = 0 → sem E/S. prazo é relativo à chegada (usado pelo EDF). */
    public record Proc(String nome, int chegada, int burst, int ioApos, int ioDuracao, int prazo) {}

    public record Cenario(String nome, String descricao, List<Proc> processos) {}

    public static final List<Cenario> TODOS = List.of(
        new Cenario("Cenário 1: processos curtos e semelhantes",
            "4 processos de 3 a 4 unidades, chegando quase juntos.",
            List.of(
                new Proc("A", 0, 4, 0, 0, 15),
                new Proc("B", 0, 3, 0, 0, 15),
                new Proc("C", 1, 4, 0, 0, 15),
                new Proc("D", 2, 3, 0, 0, 15))),

        new Cenario("Cenário 2: processos muito diferentes, chegando em tempos diferentes",
            "Um processo longo (12) chega primeiro; curtos e médios chegam logo depois.",
            List.of(
                new Proc("Longo",   0, 12, 0, 0, 40),
                new Proc("Curto1",  1,  2, 0, 0,  6),
                new Proc("Medio1",  2,  6, 0, 0, 20),
                new Proc("Curto2",  3,  1, 0, 0,  5),
                new Proc("Medio2",  4,  5, 0, 0, 22))),

        new Cenario("Cenário 3: processos com bloqueio de E/S",
            "Processos que bloqueiam após alguma execução e voltam depois da E/S.",
            List.of(
                new Proc("Editor",     0, 6, 2, 4, 20),
                new Proc("Download",   0, 5, 1, 5, 25),
                new Proc("Compilador", 1, 8, 0, 0, 30),
                new Proc("Backup",     2, 4, 3, 3, 18)))
    );

    /** Agenda todos os processos do cenário no kernel. */
    public static void carregar(Kernel k, Cenario c) {
        for (var p : c.processos())
            k.agendar(p.nome(), p.chegada(), p.burst(), p.ioApos(), p.ioDuracao(), p.prazo());
    }
}
