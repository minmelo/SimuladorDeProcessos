package Sistema;

// deadlineRel = prazo relativo ao momento em que o job nasce
public record TarefaSistema(int id, String nome, int periodo, int burst, int deadlineRel) {}
