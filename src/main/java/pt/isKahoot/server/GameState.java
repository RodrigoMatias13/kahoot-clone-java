package pt.isKahoot.server;

import pt.isKahoot.shared.Question;
import pt.isKahoot.shared.GameStartBarrier;

import java.util.*;
/**
 * Estado partilhado de um jogo ativo.
 *
 * Guarda:
 *  - equipas
 *  - jogadores
 *  - respostas
 *  - pontuações
 *  - handlers associados
 *
 * Todos os acessos concorrentes são protegidos
 * através de métodos synchronized.
 */

public class GameState {
    private final String gameCode;
    private final int numTeams;
    private final int playersPerTeam;
    private final List<Question> questions;

 // Estruturas principais do estado do jogo
    
    private final Map<String, List<String>> teams = new HashMap<>();
    private final Map<String, String> playerTeam = new HashMap<>();
    private final Map<String, Integer> teamScores = new HashMap<>();

    private final Map<String, Integer> currentAnswers = new HashMap<>();
    private final Map<String, Integer> currentFactors = new HashMap<>();

    private final Map<String, DealWithClient> handlers = new HashMap<>();
    private final GameStartBarrier startBarrier;
    private int currentQuestionIndex = 0;

    public GameState(String gameCode,
            int numTeams,
            int playersPerTeam,
            List<Question> questions) {

    		this.gameCode = gameCode;
    		this.numTeams = numTeams;
    		this.playersPerTeam = playersPerTeam;
    		this.questions = questions;

    		this.startBarrier = new GameStartBarrier(numTeams * playersPerTeam);
    		}

    /**
     * Regista um novo jogador no jogo.
     *
     * Valida:
     *  - usernames duplicados
     *  - limite de equipas
     *  - limite de jogadores por equipa
     *  - estado do jogo
     */
    
    public synchronized GameState.JoinResult registerPlayer(String username, String teamCode, DealWithClient handler) {
        if (playerTeam.containsKey(username)) return JoinResult.DUPLICATE_USERNAME;
        if (!teams.containsKey(teamCode) && teams.size() >= numTeams) return JoinResult.GAME_FULL;

        teams.putIfAbsent(teamCode, new ArrayList<>());
        List<String> team = teams.get(teamCode);
        if (team.size() >= playersPerTeam) return JoinResult.TEAM_FULL;

        team.add(username);
        playerTeam.put(username, teamCode);
        teamScores.putIfAbsent(teamCode, 0);
        handlers.put(username, handler);
        return JoinResult.OK;
    }

 // Remove respostas e fatores da ronda anterior
    
    public synchronized void resetRound() {
        currentAnswers.clear();
        currentFactors.clear();
    }

    public synchronized void recordAnswer(String username, int answer, int factor) {
        if (!currentAnswers.containsKey(username)) {
            currentAnswers.put(username, answer);
            currentFactors.put(username, factor);
        }
    }

    public synchronized Map<String, Integer> getCurrentAnswers() { return new HashMap<>(currentAnswers); }
    public synchronized Map<String, Integer> getCurrentFactors() { return new HashMap<>(currentFactors); }
    public synchronized void addScore(String teamCode, int points) {
        teamScores.put(teamCode,
            teamScores.getOrDefault(teamCode, 0) + points);
    }

    public synchronized Map<String, Integer> getScores() {
        return new HashMap<>(teamScores);
    }
    
    public synchronized int currentPlayerCount() { return playerTeam.size(); }
    public synchronized int totalExpectedPlayers() { return numTeams * playersPerTeam; }
    public synchronized boolean allPlayersJoined() { return playerTeam.size() >= totalExpectedPlayers(); }
    

    public Question getCurrentQuestion(int idx) { return questions.get(idx); }
    public int getTotalQuestions() { return questions.size(); }
    public String getGameCode() { return gameCode; }
    public GameStartBarrier getStartBarrier() { return startBarrier; }
    public synchronized Map<String, List<String>> getTeams() { return new HashMap<>(teams); }
    public synchronized Collection<DealWithClient> getHandlers() { return new ArrayList<>(handlers.values()); }
    public synchronized DealWithClient getHandler(String username) { return handlers.get(username); }

    public synchronized int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public synchronized void setCurrentQuestionIndex(int idx) {
        this.currentQuestionIndex = idx;
    }
    public enum JoinResult { OK, DUPLICATE_USERNAME, GAME_FULL, TEAM_FULL }
}