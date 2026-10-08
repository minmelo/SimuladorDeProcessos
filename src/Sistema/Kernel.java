package Sistema;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import Apps.*;
import Escalonador.protocolEscalonador;
import Processos.*;

public class Kernel implements Lancador {
    private static final int PRAZO_USUARIO = 200;
    private static final int ID_USUARIO = 100;

    private final protocolEscalonador esc;
    private final int tickMs;
    private final Scanner entrada;

    private final List<TarefaSistema> tarefas = List.of(
        new TarefaSistema(1, "Kernel",              120, 1, 120),
        new TarefaSistema(2, "Gerenciador memória", 20, 2, 20),
        new TarefaSistema(3, "Sistema de arquivos", 25, 2, 25),
        new TarefaSistema(4, "Relógio/Interface",    5, 1,  5)
    );

    private final Map<String, Aplicativo> apps = new LinkedHashMap<>();
    private final Map<Integer, ProcessosProtocol> ativos = new LinkedHashMap<>();
    private final List<ProcessosProtocol> bloqueados = new ArrayList<>();
    private final Queue<String> comandos = new ConcurrentLinkedQueue<>();

    private volatile int relogio = 0;
    private volatile boolean ligado = true;
    private int proximoId = ID_USUARIO;
    private int ultimoId = -1;
    private boolean ocioso = false;
    private boolean mostrarSistema = true;
    private int deadlinesPerdidos = 0;

    public Kernel(protocolEscalonador esc, int tickMs, Scanner entrada) {
        this.esc = esc;
        this.tickMs = tickMs;
        this.entrada = entrada;
        for (Aplicativo a : List.of(new Jogo(this), new RedeSocial(this),
                                    new Calculadora(this), new Navegador(this))) {
            apps.put(a.nome(), a);
        }
    }

    // ---------- ciclo de vida ----------
    public void iniciar() {
        iniciarLeitor();
        new DaemonSistema(this, tarefas, tickMs).start();
        IO.println("SO iniciado com " + esc.nome() + ". Digite 'ajuda'.");
        while (ligado) {
            tick();
            dormir();
        }
        IO.println("SO encerrado em t=" + relogio
            + " (deadlines perdidos: " + deadlinesPerdidos + ")");
    }

    private synchronized void tick() {
        processarComandos();
        desbloquear();
        executarCpu();
    }

    private void iniciarLeitor() {
        var t = new Thread(() -> {
            while (entrada.hasNextLine()) comandos.add(entrada.nextLine());
            comandos.add("sair");
        });
        t.setDaemon(true);
        t.start();
    }

    // ---------- API usada pelos apps e pelo daemon ----------
    public int agora() { return relogio; }

    public synchronized int lancar(String nome, int burst, int ioApos, int ioDuracao) {
        int id = proximoId++;
        admitir(new Processo(id, nome, relogio, burst,
                             relogio + PRAZO_USUARIO, ioApos, ioDuracao));
        return id;
    }

    public synchronized void liberarSistema(TarefaSistema t) {
        if (ativos.containsKey(t.id())) {
            deadlinesPerdidos++;
            if (mostrarSistema)
                IO.println("tempo " + relogio + ": ATRASO, " + t.nome() + " ainda não terminou");
            return;
        }
        admitir(new Processo(t.id(), t.nome(), relogio, t.burst(),
                             relogio + t.deadlineRel()));
    }

    // ---------- núcleo da simulação ----------
    private void admitir(ProcessosProtocol p) {
        p.setEstado(Estados.PRONTO);
        ativos.put(p.getId(), p);
        esc.admitir(p);
        log(p, "chegou à fila de prontos");
    }

    private void desbloquear() {
        var it = bloqueados.iterator();
        while (it.hasNext()) {
            var p = it.next();
            if (relogio >= p.getBloqueadoAte()) {
                it.remove();
                p.setEstado(Estados.PRONTO);
                esc.admitir(p);
                log(p, "terminou a E/S e voltou à fila de prontos");
            }
        }
    }

    private void executarCpu() {
        var p = esc.escolher();
        if (p == null) {
            if (!ocioso) { IO.println("tempo " + relogio + ": CPU ociosa"); ocioso = true; }
            ultimoId = -1;
            relogio++;
            return;
        }
        ocioso = false;

        log(p, p.getId() == ultimoId ? "continua em execução" : "entrou em execução");
        p.setEstado(Estados.EM_EXECUCAO);
        p.registrarInicio(relogio);
        p.executar(1);
        ultimoId = p.getId();
        relogio++;

        if (p.terminou()) {
            p.registrarConclusao(relogio);
            ativos.remove(p.getId());
            boolean perdeu = p.getConclusao() > p.getDeadline();
            if (perdeu) deadlinesPerdidos++;
            log(p, "concluído (espera=" + p.getEspera() + ", retorno=" + p.getRetorno() + ")"
                + (perdeu ? " [PERDEU DEADLINE]" : ""));
            esc.aposTick(p);
            ultimoId = -1;
        } else if (p.deveBloquear()) {
            p.bloquear(relogio);
            bloqueados.add(p);
            esc.aposTick(p);
            esc.remover(p.getId());            // sai da fila de prontos
            log(p, "foi bloqueado (E/S até t=" + p.getBloqueadoAte() + ")");
            ultimoId = -1;
        } else {
            p.setEstado(Estados.PRONTO);
            esc.aposTick(p);
        }
    }

    private void log(ProcessosProtocol p, String msg) {
        if (p.getId() < ID_USUARIO && !mostrarSistema) return;
        IO.println("tempo " + relogio + ": processo " + p.getId()
            + " (" + p.getNome() + ") " + msg);
    }

    // ---------- terminal ----------
    private void processarComandos() {
        String linha;
        while ((linha = comandos.poll()) != null) {
            var a = linha.trim().split("\\s+");
            if (a[0].isEmpty()) continue;
            String cmd = a[0].toLowerCase();

            if (apps.containsKey(cmd)) {
                var app = apps.get(cmd);
                if (a.length < 2 || !app.executar(a[1], Arrays.copyOfRange(a, 2, a.length)))
                    IO.println("Ações de " + cmd + ": " + app.acoes());
                continue;
            }
            switch (cmd) {
                case "ps"         -> listar();
                case "fechar"     -> fechar(a);
                case "logsistema" -> mostrarSistema = a.length > 1 && a[1].equalsIgnoreCase("on");
                case "ajuda"      -> ajuda();
                case "sair"       -> ligado = false;
                default           -> IO.println("Comando desconhecido. Digite 'ajuda'.");
            }
        }
    }

    private void fechar(String[] a) {
        try {
            int id = Integer.parseInt(a[1]);
            if (id < ID_USUARIO) { IO.println("Processo do sistema não pode ser encerrado."); return; }
            var p = ativos.remove(id);
            if (p == null) { IO.println("Processo " + id + " não existe."); return; }
            bloqueados.remove(p);
            esc.remover(id);
            p.registrarConclusao(relogio);
            if (ultimoId == id) ultimoId = -1;
            log(p, "foi encerrado pelo usuário");
        } catch (RuntimeException e) {
            IO.println("Uso: fechar <id>");
        }
    }

    private void listar() {
        IO.println("t=" + relogio + " | " + esc.nome());
        IO.println("  ID  NOME                       CHEGADA TOTAL REST  ESTADO");
        for (var p : ativos.values()) {
            IO.println(String.format("%s %-3d %-26s %-7d %-5d %-5d %s",
                p.getId() == ultimoId ? "*" : " ", p.getId(), p.getNome(),
                p.getChegada(), p.getTempoTotal(), p.getTempoRestante(), p.getEstado()));
        }
    }

    private void ajuda() {
        IO.println("<app> <ação> | ps | fechar <id> | logsistema on|off | sair");
        apps.values().forEach(a -> IO.println("  " + a.nome() + ": " + a.acoes()));
        IO.println("  calculadora calcular 2 + 3");
    }

    private void dormir() {
        try { Thread.sleep(tickMs); }
        catch (InterruptedException e) { ligado = false; }
    }
}
