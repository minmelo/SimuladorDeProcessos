package Sistema;

public interface Lancador {
    // cria um processo; ioApos = 0 significa sem E/S
    int lancar(String nome, int burst, int ioApos, int ioDuracao);
    int agora();
}