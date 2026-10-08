import java.util.*;
import Escalonador.*;
import Sistema.*;

int lerInt(Scanner in, String msg) {
    while (true) {
        IO.print(msg);
        try {
            int v = Integer.parseInt(in.nextLine().trim());
            if (v > 0) return v;
        } catch (NumberFormatException e) { }
        IO.println("Digite um inteiro > 0.");
    }
}

void main() {
    var in = new Scanner(System.in);
    protocolEscalonador alg = null;
    while (alg == null) {
        IO.println("\n=== Simulador de SO ===");
        IO.println("1) Executar com FCFS");
        IO.println("2) Executar com Round Robin");
        IO.println("3) Executar com EDF (tempo real)");
        switch (in.nextLine().trim()) {
            case "1" -> alg = new Fcfs();
            case "2" -> alg = new RoundRobin(lerInt(in, "Quantum: "));
            case "3" -> alg = new Edf();
            default  -> IO.println("Opção inválida.");
        }
    }
    new Kernel(alg, 500, in).iniciar();
}
