package pt.isKahoot.shared;

import java.io.Serializable;
import java.util.List;
import java.util.Map;


/**
 * Classe utilizada para comunicação entre cliente e servidor.
 * Todas as mensagens trocadas no jogo passam por esta estrutura.
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    // Tipos de mensagens possíveis
	public enum Type {
        // Client -> Server
        JOIN,           // player joins a game
        ANSWER,         // player sends an answer

        // Server -> Client
        JOIN_OK,        // join accepted
        JOIN_FAIL,      // join rejected (reason in data)
        QUESTION,       // new question
        ROUND_RESULT,   // scoreboard after a round
        GAME_OVER,      // final scoreboard
        WAITING,        // waiting for other players to join
    }

	// Tipo da mensagem
    private Type type;

    // Código do jogo
    private String gameCode;

    // Código da equipa
    private String teamCode;

    // Nome do jogador
    private String username;

    // Resposta escolhida
    private int answer;

    // Informação da pergunta
    private QuestionPayload questionPayload;

    // Informação das pontuações
    private ScorePayload scorePayload;

    // Texto genérico (ex.: erros)
    private String text;

    //  ---Método de criação---

    // Cria mensagem de entrada num jogo.
    public static Message join(String gameCode, String teamCode, String username) {
        Message m = new Message();
        m.type = Type.JOIN;
        m.gameCode = gameCode;
        m.teamCode = teamCode;
        m.username = username;
        return m;
    }

    // Mensagem de entrada aceite
    public static Message joinOk() {
        Message m = new Message();
        m.type = Type.JOIN_OK;
        return m;
    }

    // Mensagem de entrada rejeitada
    public static Message joinFail(String reason) {
        Message m = new Message();
        m.type = Type.JOIN_FAIL;
        m.text = reason;
        return m;
    }
    
    // Mensagem de espera
    public static Message waiting() {
        Message m = new Message();
        m.type = Type.WAITING;
        return m;
    }

    //Mensagem de nova pergunta
    public static Message question(QuestionPayload payload) {
        Message m = new Message();
        m.type = Type.QUESTION;
        m.questionPayload = payload;
        return m;
    }

    // Mensagem com resposta do jogador
    public static Message answer(int answer) {
        Message m = new Message();
        m.type = Type.ANSWER;
        m.answer = answer;
        return m;
    }

    // Mensagem com resultado da ronda
    public static Message roundResult(ScorePayload payload) {
        Message m = new Message();
        m.type = Type.ROUND_RESULT;
        m.scorePayload = payload;
        return m;
    }

    // Mensagem de fim de jogo
    public static Message gameOver(ScorePayload payload) {
        Message m = new Message();
        m.type = Type.GAME_OVER;
        m.scorePayload = payload;
        return m;
    }

    // --- Getters ---

    public Type getType() { return type; }
    public String getGameCode() { return gameCode; }
    public String getTeamCode() { return teamCode; }
    public String getUsername() { return username; }
    public int getAnswer() { return answer; }
    public QuestionPayload getQuestionPayload() { return questionPayload; }
    public ScorePayload getScorePayload() { return scorePayload; }
    public String getText() { return text; }

    // --- Classe interna para dados das perguntas ---

    public static class QuestionPayload implements Serializable {
        private static final long serialVersionUID = 1L;
     // Número atual da pergunta
        public int questionNumber;

        // Número total de perguntas
        public int totalQuestions;

        // Texto da pergunta
        public String questionText;

        // Opções possíveis
        public List<String> options;

        // Pontos da pergunta
        public int points;

        // Tempo limite para responder
        public int timeLimitSeconds;

        // Indica se é ronda de equipa
        public boolean isTeamRound;

        public QuestionPayload(int questionNumber, int totalQuestions, String questionText,
                               List<String> options, int points, int timeLimitSeconds, boolean isTeamRound) {
            this.questionNumber = questionNumber;
            this.totalQuestions = totalQuestions;
            this.questionText = questionText;
            this.options = options;
            this.points = points;
            this.timeLimitSeconds = timeLimitSeconds;
            this.isTeamRound = isTeamRound;
        }
    }

    // =====================================================
    // CLASSE INTERNA PARA DADOS DAS PONTUAÇÕES
    // =====================================================
    
    public static class ScorePayload implements Serializable {
        private static final long serialVersionUID = 1L;
        // Pontos obtidos nesta ronda
        public int roundPoints;

        // Pontuação total das equipas
        public Map<String, Integer> scores;

        // Pergunta atual
        public int questionNumber;

        // Número total de perguntas
        public int totalQuestions;

        public ScorePayload(int roundPoints, Map<String, Integer> scores, int questionNumber, int totalQuestions) {
            this.roundPoints = roundPoints;
            this.scores = scores;
            this.questionNumber = questionNumber;
            this.totalQuestions = totalQuestions;
        }
    }
}
