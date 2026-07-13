package pt.isKahoot.server;

import pt.isKahoot.shared.*;
import java.util.*;

/**
 * Thread responsável pela execução completa de um jogo.
 *
 * Responsabilidades:
 *  - coordenar rondas
 *  - alternar entre perguntas individuais e de equipa
 *  - controlar temporizadores
 *  - calcular pontuações
 *  - enviar classificações aos jogadores
 */

public class GameHandler implements Runnable {
    private static final int TIMER_ROUND_MS = 30_000;
    private final GameState state;

    public GameHandler(GameState state) {
        this.state = state;
    }

    @Override
    public void run() {
        try {

        	
        	/*
             * Espera até todos os clientes
             * estarem prontos para receber
             * mensagens.
             */
            while (!allPlayersReady()) {
                Thread.sleep(10);
            }
            
            
            int totalQuestions = state.getTotalQuestions();
            
            /*
             * Ciclo principal das perguntas do jogo.
             * As rondas alternam entre:
             *  - individuais
             *  - equipa
             */
            
            for (int i = 0; i < totalQuestions; i++) {

                state.setCurrentQuestionIndex(i);

                Question q = state.getCurrentQuestion(i);
                boolean isTeamRound = (i % 2 == 1);

             // Limpa respostas e fatores da ronda anterior
                
                state.resetRound();
                int expectedAnswers = state.currentPlayerCount();
                
                /*
                 * Latch global da ronda.
                 * Permite bloquear a execução até:
                 *  - todas as respostas chegarem
                 *  - ou o tempo limite expirar
                 */
                
                ModifiedCountdownLatch globalRoundLatch = new ModifiedCountdownLatch(1, 0, TIMER_ROUND_MS, expectedAnswers);
                ModifiedCountdownLatch indLatch = null;
                Map<String, TeamBarrier> barriers = new HashMap<>();

                if (!isTeamRound) {
                    indLatch = new ModifiedCountdownLatch(2, 2, TIMER_ROUND_MS, expectedAnswers);
                    for (DealWithClient dwc : state.getHandlers()) {
                        dwc.setLatches(indLatch, globalRoundLatch);
                    }
                } else {
                    Map<String, List<String>> teams = state.getTeams();
                    for (Map.Entry<String, List<String>> entry : teams.entrySet()) {
                        TeamBarrier barrier = new TeamBarrier(entry.getValue().size(), TIMER_ROUND_MS, null);
                        barriers.put(entry.getKey(), barrier);
                        for (String user : entry.getValue()) {
                            DealWithClient dwc = state.getHandler(user);
                            if (dwc != null) dwc.setBarrier(barrier, globalRoundLatch);
                        }
                    }
                }

                Message.QuestionPayload qp = new Message.QuestionPayload(
                    i + 1, totalQuestions, q.getQuestion(), q.getOptions(), q.getPoints(), TIMER_ROUND_MS / 1000, isTeamRound
                );
                broadcast(Message.question(qp));

                // Bloqueia com precisão absoluta até os 30s ou até todas as respostas entrarem
                globalRoundLatch.await();

                // Forçar libertação de barreiras se o tempo expirar
                if (isTeamRound) {
                    for (TeamBarrier b : barriers.values()) b.forceRelease();
                } else if (indLatch != null) {
                    indLatch.forceExpire();
                }

                // Processar e enviar placar da ronda
                Map<String, Integer> roundScores = processScores(q, isTeamRound);
                roundScores.forEach(state::addScore);

                Map<String, Integer> absoluteTotals = state.getScores();
                int qNum = i + 1;

                state.getTeams().forEach((teamCode, members) -> {
                    int earned = roundScores.getOrDefault(teamCode, 0);
                    Message.ScorePayload sp = new Message.ScorePayload(earned, absoluteTotals, qNum, totalQuestions);
                    for (String user : members) {
                        DealWithClient dwc = state.getHandler(user);
                        if (dwc != null) dwc.send(Message.roundResult(sp));
                    }
                });

                Thread.sleep(3000); // Pausa para visualizar o score
            }

            // Fim do Jogo
            Message.ScorePayload finalPayload = new Message.ScorePayload(0, state.getScores(), totalQuestions, totalQuestions);
            broadcast(Message.gameOver(finalPayload));
            state.getHandlers().forEach(DealWithClient::stopRunning);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, Integer> processScores(Question q, boolean isTeamRound) {
        Map<String, Integer> roundPoints = new HashMap<>();
        Map<String, Integer> answers = state.getCurrentAnswers();
        Map<String, Integer> factors = state.getCurrentFactors();

        state.getTeams().forEach((teamCode, members) -> {
            if (!isTeamRound) {
                int scoreSum = 0;
                for (String user : members) {
                    Integer ans = answers.get(user);
                    Integer fac = factors.getOrDefault(user, 1);
                    // IMPORTANTE: Alinhamento de Base-1 da GUI (1 a 4) com Base-0 do JSON do enunciado [cite: 107]
                    if (ans != null && ans == q.getCorrect()) {
                        scoreSum += q.getPoints() * fac;
                    }
                }
                roundPoints.put(teamCode, scoreSum);
            } else {
            	int correctCount = 0;

            	for (String user : members) {

            	    Integer ans = answers.get(user);

            	    // Converter resposta da GUI (1-4) para índice (0-3)
            	    if (ans != null && ans == q.getCorrect()) {
            	        correctCount++;
            	    }
            	}

            	int finalPts = 0;

            	// Todos acertaram
            	if (correctCount == members.size()) {

            	    finalPts = (q.getPoints() * members.size()) * 2;

            	}
            	// Apenas um acertou
            	else if (correctCount > 0) {

            	    finalPts = q.getPoints() * correctCount;

            	}
            	// Ninguém acertou
            	else {

            	    finalPts = 0;
            	}

            	roundPoints.put(teamCode, finalPts);
            }
        });
        return roundPoints;
    }

    private void broadcast(Message m) {
        for (DealWithClient dwc : state.getHandlers()) dwc.send(m);
    }
    
    private boolean allPlayersReady() {

        for (DealWithClient dwc : state.getHandlers()) {

            if (!dwc.isReady()) {
                return false;
            }
        }

        return true;
    }
    
}