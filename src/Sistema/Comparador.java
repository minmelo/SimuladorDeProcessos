package Sistema;

import java.util.*;
import java.util.function.Supplier;
import Escalonador.*;

/** Roda cada cenário com cada algoritmo e imprime uma tabela comparativa. */
public final class Comparador {
    private static final Locale PT = Locale.forLanguageTag("pt-BR");

    private Comparador() {}

    public static void comparar(int quantum) {
        // cada execução precisa de um escalonador novo (eles guardam estado)
        List<Supplier<protocolEscalonador>> algoritmos = List.of(
            Fcfs::new, () -> new RoundRobin(quantum), Edf::new);

        for (var cenario : Cenarios.TODOS) {
            System.out.println("\n" + cenario.nome());
            System.out.println(cenario.descricao());
            System.out.println(String.format(PT, "%-18s %6s %11s %9s %6s %6s %11s %7s %9s",
                "Algoritmo", "Concl.", "Turnaround", "Resposta", "Tempo", "CPU%",
                "Throughput", "Trocas", "Perdidos"));

            var resumos = new ArrayList<Metricas.Resumo>();
            for (var novo : algoritmos) {
                var kernel = new Kernel(novo.get(), false);     // sem log da linha do tempo
                Cenarios.carregar(kernel, cenario);
                kernel.iniciar();
                var r = kernel.resumo();
                resumos.add(r);
                System.out.println(String.format(PT, "%-18s %6d %11.2f %9.2f %6d %5.1f%% %11.3f %7d %9d",
                    r.algoritmo(), r.concluidos(), r.turnaroundMedio(), r.respostaMedio(),
                    r.tempoTotal(), r.utilizacaoCpu() * 100, r.throughput(),
                    r.trocasContexto(), r.deadlinesPerdidos()));
            }
            System.out.println("Menor turnaround médio: "
                + melhores(resumos, Metricas.Resumo::turnaroundMedio));
            System.out.println("Menor tempo de resposta médio: "
                + melhores(resumos, Metricas.Resumo::respostaMedio));
        }
    }

    private static String melhores(List<Metricas.Resumo> rs,
                                   java.util.function.ToDoubleFunction<Metricas.Resumo> f) {
        double min = rs.stream().mapToDouble(f).min().orElse(0);
        var nomes = new ArrayList<String>();
        for (var r : rs) if (Math.abs(f.applyAsDouble(r) - min) < 1e-9) nomes.add(r.algoritmo());
        return String.join(", ", nomes) + String.format(PT, " (%.2f)", min);
    }
}
