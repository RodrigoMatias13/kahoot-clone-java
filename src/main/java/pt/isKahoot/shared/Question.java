package pt.isKahoot.shared;

import java.io.Serializable;
import java.util.List;

//Classe que representa uma pergunta do jogo

public class Question implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Texto da pergunta
    private String question;

    // Pontos atribuídos
    private int points;

    // Índice da resposta correta
    private int correct;

    // Lista de opções
    private List<String> options;

    public String getQuestion() { return question; }
    public int getPoints() { return points; }
    public int getCorrect() { return correct; }
    public List<String> getOptions() { return options; }

    // Verifica se a resposta está correta
    
    public boolean isCorrect(int answer) {
        return answer == correct;
    }

    @Override
    public String toString() {
        return "Question{question='" + question + "', points=" + points + ", correct=" + correct + "}";
    }
}
