package Apps;

import java.util.List;
import Sistema.Lancador;

public class Jogo extends Aplicativo {
    public Jogo(Lancador so) { super(so); }

    public String nome() { return "jogo"; }
    public List<String> acoes() { return List.of("andar", "pular", "atacar"); }

    public void andar()  { so.lancar("jogo:andar", 2, 0, 0); }
    public void pular()  { so.lancar("jogo:pular", 1, 0, 0); }
    public void atacar() { so.lancar("jogo:atacar", 3, 0, 0); }

    public boolean executar(String acao, String[] args) {
        return switch (acao.toLowerCase()) {
            case "andar"  -> { andar();  yield true; }
            case "pular"  -> { pular();  yield true; }
            case "atacar" -> { atacar(); yield true; }
            default -> false;
        };
    }
}
