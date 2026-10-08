package Apps;

import java.util.List;
import Sistema.Lancador;

public class Calculadora extends Aplicativo {
    public Calculadora(Lancador so) { super(so); }

    public String nome() { return "calculadora"; }
    public List<String> acoes() { return List.of("calcular"); }

    // uso: calculadora calcular 2 + 3
    public void calcular(String[] a) {
        if (a.length == 3) {
            try {
                double x = Double.parseDouble(a[0]), y = Double.parseDouble(a[2]);
                double r = switch (a[1]) {
                    case "+" -> x + y;
                    case "-" -> x - y;
                    case "*" -> x * y;
                    case "/" -> x / y;
                    default -> Double.NaN;
                };
                IO.println("= " + r);
            } catch (NumberFormatException e) {
                IO.println("Números inválidos.");
            }
        }
        so.lancar("calculadora:calcular", 2, 0, 0);
    }

    public boolean executar(String acao, String[] args) {
        if (!acao.equalsIgnoreCase("calcular")) return false;
        calcular(args);
        return true;
    }
}
