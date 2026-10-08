package Sistema;

import java.util.List;

public class DaemonSistema extends Thread {
    private final Kernel kernel;
    private final List<TarefaSistema> tarefas;
    private final int pausaMs;

    public DaemonSistema(Kernel kernel, List<TarefaSistema> tarefas, int tickMs) {
        this.kernel = kernel;
        this.tarefas = tarefas;
        this.pausaMs = Math.max(1, tickMs / 4);
        setDaemon(true);
        setName("daemon-sistema");
    }

    @Override
    public void run() {
        int[] proxima = new int[tarefas.size()];      // todas liberam em t=0
        try {
            while (!isInterrupted()) {
                int agora = kernel.agora();
                for (int i = 0; i < tarefas.size(); i++) {
                    var t = tarefas.get(i);
                    if (agora >= proxima[i]) {
                        kernel.liberarSistema(t);
                        proxima[i] = (agora / t.periodo() + 1) * t.periodo();
                    }
                }
                Thread.sleep(pausaMs);
            }
        } catch (InterruptedException e) { /* encerra */ }
    }
}
