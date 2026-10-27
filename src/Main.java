import java.util.*;
import java.util.function.Supplier;
import Escalonador.*;
import Sistema.*;

int lerInt(Scanner in, String msg) {
    while (true) {
        System.out.print(msg);
        try {
            int v = Integer.parseInt(in.nextLine().trim());
            if (v > 0) return v;
        } catch (NumberFormatException e) { }
        System.out.println("Digite um inteiro > 0.");
    }
}

Supplier<protocolEscalonador> escolherAlgoritmo(Scanner in) {
    while (true) {
        System.out.println("Algoritmo: 1) FCFS  2) Round Robin  3) EDF (tempo real)");
        switch (in.nextLine().trim()) {
            case "1" -> { return Fcfs::new; }
            case "2" -> { int q = lerInt(in, "Quantum: "); return () -> new RoundRobin(q); }
            case "3" -> { return Edf::new; }
            default  -> System.out.println("Opção inválida.");
        }
    }
}

void executarCenario(Scanner in) {
    var lista = Cenarios.TODOS;
    for (int i = 0; i < lista.size(); i++)
        System.out.println((i + 1) + ") " + lista.get(i).nome());
    int c = 0;
    while (c < 1 || c > lista.size()) c = lerInt(in, "Cenário: ");
    var algoritmo = escolherAlgoritmo(in);

    var kernel = new Kernel(algoritmo.get(), true);   // mostra linha do tempo e métricas
    Cenarios.carregar(kernel, lista.get(c - 1));
    kernel.iniciar();
}

void menuTerminal() {
    var in = new Scanner(System.in);
    while (true) {
        System.out.println("\n=== Simulador de SO ===");
        System.out.println("1) Executar com FCFS (interativo)");
        System.out.println("2) Executar com Round Robin (interativo)");
        System.out.println("3) Executar com EDF (interativo, tempo real)");
        System.out.println("4) Comparar algoritmos nos cenários de teste");
        System.out.println("5) Executar um cenário de teste com linha do tempo");
        System.out.println("6) Abrir interface gráfica (navegador)");
        System.out.println("0) Sair");
        if (!in.hasNextLine()) return;
        switch (in.nextLine().trim()) {
            case "1" -> { new Kernel(new Fcfs(), 500, in).iniciar(); return; }
            case "2" -> { new Kernel(new RoundRobin(lerInt(in, "Quantum: ")), 500, in).iniciar(); return; }
            case "3" -> { new Kernel(new Edf(), 500, in).iniciar(); return; }
            case "4" -> Cenarios_comparar(in);
            case "5" -> executarCenario(in);
            case "6" -> { try { Servidor.iniciar(8080); } catch (Exception e) { System.out.println("Erro: " + e); } return; }
            case "0" -> { return; }
            default  -> System.out.println("Opção inválida.");
        }
    }
}

void Cenarios_comparar(Scanner in) {
    Comparador.comparar(lerInt(in, "Quantum do Round Robin: "));
}

void abrirNavegador(String url) {
    try {
        if (java.awt.Desktop.isDesktopSupported()
                && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            return;
        }
    } catch (Exception e) { }
    System.out.println("Abra no navegador: " + url);
}

// Padrão: sobe o servidor e abre o navegador. Com o argumento "terminal", mostra o menu textual.
void main(String[] args) {
    if (args.length > 0 && args[0].equalsIgnoreCase("terminal")) {
        menuTerminal();
        return;
    }
    try {
        Servidor.iniciar(8080);
        abrirNavegador("http://localhost:8080");
    } catch (Exception e) {
        System.out.println("Erro ao iniciar a interface: " + e);
    }
}