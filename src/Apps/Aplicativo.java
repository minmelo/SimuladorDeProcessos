package Apps;

import java.util.List;
import Sistema.Lancador;

public abstract class Aplicativo {
    protected final Lancador so;
    protected Aplicativo(Lancador so) { this.so = so; }

    public abstract String nome();
    public abstract List<String> acoes();
    public abstract boolean executar(String acao, String[] args);
}
