package Processos;

public interface ProcessosProtocol {
    int getId();
    String getNome();
    int getChegada();
    int getTempoTotal();
    int getTempoRestante();
    int getDeadline();          // usado pelo EDF (absoluto)
    Estados getEstado();

    void setEstado(Estados e);
    void executar(int unidades);   // reduz tempoRestante
    boolean terminou();

    void registrarInicio(int t);
    void registrarConclusao(int t);
    int getInicio();
    int getConclusao();
    int getEspera();
    int getRetorno();
}