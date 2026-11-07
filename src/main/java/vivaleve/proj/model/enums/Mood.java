package vivaleve.proj.model.enums;

public enum Mood {
    GREAT(5),
    GOOD(4),
    NEUTRAL(3),
    BAD(2),
    AWFUL(1);

    private final int score;

    Mood(int score) {
        this.score = score;
    }

    /** Nota numérica do humor (GREAT=5 ... AWFUL=1), usada no gráfico de evolução. */
    public int getScore() {
        return score;
    }
}
