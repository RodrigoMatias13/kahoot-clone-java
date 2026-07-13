package pt.isKahoot.shared;

import java.util.List;

/**
 * Classe responsável por representar a configuração de um jogo.
 * Contém o nome do jogo e a lista de perguntas associadas.
 */

public class GameConfig {
	
	// Nome do jogo
    private String name;
    // Lista de perguntas do jogo
    private List<Question> questions;

    // Devolve o nome do jogo
    public String getName() { return name; }
    // Devolve a lista de perguntas
    public List<Question> getQuestions() { return questions; }
}
