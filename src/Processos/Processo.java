
package Processos;

public class Processo implements ProcessosProtocol {
    // variaveis imutaveis
    private final String nome;
    private final int id;
    private final int chegada;
    private final int tempoTotal;
    private final int deadline;

    //variaveis mutaveis
    private int tempoRestante;
    private int inicio = -1;      // -1 = ainda não executou
    private int conclusao = -1;
    private Estados estado = Estados.NOVO;

    public Processo(int id, String nome, int chegada, int tempoTotal, int deadline) {
        if (tempoTotal <= 0)
            throw new IllegalArgumentException("Tempo total deve ser > 0");
        this.id = id;
        this.nome = nome;
        this.chegada = chegada;
        this.tempoTotal = tempoTotal;
        this.deadline = deadline;
        this.tempoRestante = tempoTotal;
    }
    public int getId() { return id; }
    public String getNome() { return nome; }
    public int getChegada() { return chegada; }
    public int getTempoTotal() { return tempoTotal; }
    public int getTempoRestante() { return tempoRestante; }
    public int getDeadline() { return deadline; }
    public Estados getEstado() { return estado; }

    public void setEstado(Estados e) {
        this.estado = e;
    }
    
    public void executar(int unidades) {
        tempoRestante = Math.max(0, tempoRestante - unidades);
    }

    public boolean terminou() {
        return tempoRestante == 0;
    }
    
    public void registrarInicio(int t) { if (inicio < 0) inicio = t; }
    public void registrarConclusao(int t) {
        conclusao = t;
        estado = Estados.FINALIZADO;
    }
    public int getInicio() { return inicio; }
    public int getConclusao() { return conclusao; }
    public int getRetorno() { return conclusao - chegada; }
    public int getEspera() { return getRetorno() - tempoTotal; }

}