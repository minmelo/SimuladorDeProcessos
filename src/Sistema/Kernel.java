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
    private volatile int tickMs;
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

    // ---------- métricas e modo cenário ----------
    private final Metricas metricas = new Metricas();
    private final List<Processo> futuros = new ArrayList<>();   // chegadas agendadas
    private boolean verbose = true;
    private boolean encerrarAoTerminar = false;
    private boolean gui = false;
    private volatile boolean pausado = false;
    private volatile int passos = 0;
    private int fatia = 0;

    public Kernel(protocolEscalonador esc, int tickMs, Scanner entrada) {
        this.esc = esc;
        this.tickMs = tickMs;
        this.entrada = entrada;
        for (Aplicativo a : List.of(new Jogo(this), new RedeSocial(this),
                new Calculadora(this), new Navegador(this))) {
            apps.put(a.nome(), a);
        }
    }

    /** Modo interface web: sem terminal nem tarefas de sistema; controlado pelo Servidor. */
    public Kernel(protocolEscalonador esc, int tickMs) {
        this(esc, tickMs, null);
        this.gui = true;
        this.verbose = false;
    }

    /** Modo cenário: sem teclado, sem daemon, sem pausa; termina sozinho quando
     *  não há mais processos. Use agendar(...) para definir os processos. */
    public Kernel(protocolEscalonador esc, boolean verbose) {
        this(esc, 0, null);
        this.encerrarAoTerminar = true;
        this.verbose = verbose;
    }

    // ---------- ciclo de vida ----------
    public void iniciar() {
        if (encerrarAoTerminar) {
            println("Simulação iniciada com " + esc.nome() + ".");
        } else if (!gui) {
            iniciarLeitor();
            new DaemonSistema(this, tarefas, tickMs).start();
            println("SO iniciado com " + esc.nome() + ". Digite 'ajuda'.");
        }
        while (ligado) {
            if (!pausado || passos > 0) {
                if (passos > 0) passos--;
                tick();
            }
            dormir();
        }
        println("SO encerrado em t=" + relogio
                + " (deadlines perdidos: " + deadlinesPerdidos + ")");
        if (verbose) resumo().imprimir();
    }

    /** Métricas da simulação (válidas ao final da execução). */
    public Metricas.Resumo resumo() {
        return metricas.resumir(esc.nome(), relogio, deadlinesPerdidos);
    }

    private synchronized void tick() {
        processarComandos();
        admitirChegadas();
        desbloquear();
        executarCpu();
        if (encerrarAoTerminar && futuros.isEmpty() && ativos.isEmpty()) ligado = false;
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

    /** Agenda um processo para chegar no instante 'chegada' (usado nos cenários). */
    public synchronized int agendar(String nome, int chegada, int burst,
                                    int ioApos, int ioDuracao, int prazoRel) {
        int id = proximoId++;
        futuros.add(new Processo(id, nome, chegada, burst,
                chegada + prazoRel, ioApos, ioDuracao));
        return id;
    }

    public synchronized void liberarSistema(TarefaSistema t) {
        if (ativos.containsKey(t.id())) {
            deadlinesPerdidos++;
            if (mostrarSistema)
                println("tempo " + relogio + ": ATRASO, " + t.nome() + " ainda não terminou");
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

    private void admitirChegadas() {
        var it = futuros.iterator();
        while (it.hasNext()) {
            var p = it.next();
            if (p.getChegada() <= relogio) {
                it.remove();
                admitir(p);
            }
        }
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
            if (!ocioso) { println("tempo " + relogio + ": CPU ociosa"); ocioso = true; }
            ultimoId = -1;
            relogio++;
            return;
        }
        ocioso = false;
        fatia = (p.getId() == ultimoId) ? fatia + 1 : 1;
        metricas.registrarExecucao(p);

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
            if (p.getId() >= ID_USUARIO) metricas.registrarConclusao(p);
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
        println("tempo " + relogio + ": processo " + p.getId()
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
                    println("Ações de " + cmd + ": " + app.acoes());
                continue;
            }
            switch (cmd) {
                case "ps"         -> listar();
                case "fechar"     -> fechar(a);
                case "logsistema" -> mostrarSistema = a.length > 1 && a[1].equalsIgnoreCase("on");
                case "ajuda"      -> ajuda();
                case "sair"       -> ligado = false;
                default           -> println("Comando desconhecido. Digite 'ajuda'.");
            }
        }
    }

    private void fechar(String[] a) {
        try {
            int id = Integer.parseInt(a[1]);
            if (id < ID_USUARIO) { println("Processo do sistema não pode ser encerrado."); return; }
            var p = ativos.remove(id);
            if (p == null) { println("Processo " + id + " não existe."); return; }
            bloqueados.remove(p);
            esc.remover(id);
            p.registrarConclusao(relogio);
            if (ultimoId == id) ultimoId = -1;
            log(p, "foi encerrado pelo usuário");
        } catch (RuntimeException e) {
            println("Uso: fechar <id>");
        }
    }

    private void listar() {
        println("t=" + relogio + " | " + esc.nome());
        println("  ID  NOME                       CHEGADA TOTAL REST  ESTADO");
        for (var p : ativos.values()) {
            println(String.format("%s %-3d %-26s %-7d %-5d %-5d %s",
                    p.getId() == ultimoId ? "*" : " ", p.getId(), p.getNome(),
                    p.getChegada(), p.getTempoTotal(), p.getTempoRestante(), p.getEstado()));
        }
    }

    private void ajuda() {
        println("<app> <ação> | ps | fechar <id> | logsistema on|off | sair");
        apps.values().forEach(a -> println("  " + a.nome() + ": " + a.acoes()));
        println("  calculadora calcular 2 + 3");
    }

    // ---------- controle e estado para a interface web ----------
    public void parar() { ligado = false; }
    public void pausar(boolean v) { pausado = v; }
    public void passo() { passos++; }
    public void setTickMs(int ms) { tickMs = ms; }
    public synchronized void encerrarProcesso(int n) { fechar(new String[]{"fechar", "" + (n + ID_USUARIO - 1)}); }

    public synchronized String estadoJson() {
        var sb = new StringBuilder("{\"on\":true,\"t\":").append(relogio)
                .append(",\"pausado\":").append(pausado)
                .append(",\"alg\":\"").append(esc.nome()).append("\",\"fatia\":").append(fatia)
                .append(",\"procs\":[");
        boolean primeiro = true;
        for (var p : ativos.values()) {
            if (p.getId() < ID_USUARIO) continue;
            if (!primeiro) sb.append(',');
            primeiro = false;
            String e = bloqueados.contains(p) ? "B" : (p.getId() == ultimoId ? "E" : "P");
            sb.append("{\"n\":").append(p.getId() - ID_USUARIO + 1)
                    .append(",\"nome\":\"").append(p.getNome()).append("\",\"tot\":").append(p.getTempoTotal())
                    .append(",\"rest\":").append(p.getTempoRestante())
                    .append(",\"e\":\"").append(e).append("\"}");
        }
        var m = resumo();
        return sb.append("],\"m\":{\"conc\":").append(m.concluidos())
                .append(",\"ta\":").append(String.format(Locale.ROOT, "%.2f", m.turnaroundMedio()))
                .append(",\"resp\":").append(String.format(Locale.ROOT, "%.2f", m.respostaMedio()))
                .append(",\"cpu\":").append(String.format(Locale.ROOT, "%.1f", m.utilizacaoCpu() * 100))
                .append(",\"thr\":").append(String.format(Locale.ROOT, "%.3f", m.throughput()))
                .append(",\"ctx\":").append(m.trocasContexto()).append("}}").toString();
    }

    private void println(String s) {
        if (verbose) System.out.println(s);
    }

    private void dormir() {
        if (tickMs <= 0) return;
        try { Thread.sleep(tickMs); }
        catch (InterruptedException e) { ligado = false; }
    }
}
