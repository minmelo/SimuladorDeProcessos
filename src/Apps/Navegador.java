package Apps;

import java.util.List;
import Sistema.Lancador;

public class Navegador extends Aplicativo {
    public Navegador(Lancador so) { super(so); }

    public String nome() { return "navegador"; }
    public List<String> acoes() { return List.of("pesquisar"); }

    public void pesquisar() { so.lancar("navegador:pesquisar", 3, 1, 4); }

    public boolean executar(String acao, String[] args) {
        if (!acao.equalsIgnoreCase("pesquisar")) return false;
        pesquisar();
        return true;
    }
}
