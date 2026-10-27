package Sistema;

import java.util.*;
import Processos.ProcessosProtocol;

/** Coleta as métricas de uma simulação e gera o resumo final. */
public class Metricas {
    private static final Locale PT = Locale.forLanguageTag("pt-BR");

    private final List<ProcessosProtocol> concluidos = new ArrayList<>();
    private int ticksCpuOcupada = 0;
    private int trocasContexto = 0;
    private ProcessosProtocol ultimoExecutado = null;

    /** Chamado a cada unidade de tempo em que algum processo usa a CPU. */
    public void registrarExecucao(ProcessosProtocol p) {
        ticksCpuOcupada++;
        if (ultimoExecutado != null && ultimoExecutado != p) trocasContexto++;
        ultimoExecutado = p;
    }

    /** Chamado quando um processo termina normalmente (não conta os encerrados pelo usuário). */
    public void registrarConclusao(ProcessosProtocol p) {
        concluidos.add(p);
    }

    public Resumo resumir(String algoritmo, int tempoTotal, int deadlinesPerdidos) {
        int n = concluidos.size();
        double soma = 0, somaResp = 0;
        for (var p : concluidos) {
            soma += p.getRetorno();                        // conclusão - chegada
            somaResp += p.getInicio() - p.getChegada();    // 1ª execução - chegada
        }
        return new Resumo(
            algoritmo, n,
            n == 0 ? 0 : soma / n,
            n == 0 ? 0 : somaResp / n,
            tempoTotal,
            tempoTotal == 0 ? 0 : (double) ticksCpuOcupada / tempoTotal,
            tempoTotal == 0 ? 0 : (double) n / tempoTotal,
            trocasContexto,
            deadlinesPerdidos);
    }

    public record Resumo(String algoritmo, int concluidos, double turnaroundMedio,
                         double respostaMedio, int tempoTotal, double utilizacaoCpu,
                         double throughput, int trocasContexto, int deadlinesPerdidos) {

        public void imprimir() {
            System.out.println();
            System.out.println("===== Métricas: " + algoritmo + " =====");
            System.out.println(String.format(PT, "Processos concluídos ........ %d", concluidos));
            System.out.println(String.format(PT, "Turnaround médio ............ %.2f", turnaroundMedio));
            System.out.println(String.format(PT, "Tempo de resposta médio ..... %.2f", respostaMedio));
            System.out.println(String.format(PT, "Tempo total da simulação .... %d", tempoTotal));
            System.out.println(String.format(PT, "Utilização da CPU ........... %.1f%%", utilizacaoCpu * 100));
            System.out.println(String.format(PT, "Throughput .................. %.3f proc/unid. de tempo", throughput));
            System.out.println(String.format(PT, "Trocas de contexto .......... %d", trocasContexto));
            System.out.println(String.format(PT, "Deadlines perdidos .......... %d", deadlinesPerdidos));
        }
    }
}
