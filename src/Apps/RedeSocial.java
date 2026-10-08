package Apps;

import java.util.List;
import Sistema.Lancador;

public class RedeSocial extends Aplicativo {
    public RedeSocial(Lancador so) { super(so); }

    public String nome() { return "redesocial"; }
    public List<String> acoes() { return List.of("curtir", "seguir"); }

    // 1 unidade de CPU, depois espera a rede
    public void curtir() { so.lancar("redesocial:curtir", 2, 1, 3); }
    public void seguir() { so.lancar("redesocial:seguir", 3, 1, 4); }

    public boolean executar(String acao, String[] args) {
        return switch (acao.toLowerCase()) {
            case "curtir" -> { curtir(); yield true; }
            case "seguir" -> { seguir(); yield true; }
            default -> false;
        };
    }
}
