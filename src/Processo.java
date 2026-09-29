public class Processo {
    private final int id;
    private final int tempoInicial;
    private int tempoRestante;

    public Processo(int id, int tempoInicial) {
        if (tempoInicial <= 0) {
            throw new IllegalArgumentException("Tempo deve ser maior que zero");
        }
        this.id = id;
        this.tempoInicial = tempoInicial;
        this.tempoRestante = tempoInicial;
    }
    public boolean terminou() {
        return tempoRestante == 0;
    }

    public int getId() {
        return id;
    }

    public int getTempoInicial() {
        return tempoInicial;
    }

    public int getTempoRestante() {
        return tempoRestante;
    }

    @Override
    public String toString() {
        return "Processo{" +
                "id=" + id +
                ", tempoInicial=" + tempoInicial +
                ", tempoRestante=" + tempoRestante +
                '}';
    }
}
