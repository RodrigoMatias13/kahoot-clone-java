package pt.isKahoot.shared;

/**
 * Classe semelhante a um CountdownLatch,
 * mas com suporte para:
 *
 * - limite de tempo
 * - bónus para respostas rápidas
 */

public class ModifiedCountdownLatch {
	// Multiplicador de bónus
    private final int bonusFactor;

    // Número de jogadores que recebem bónus
    private final int bonusCount;

    // Tempo máximo de espera
    private final int waitPeriod;

    // Número de respostas em falta
    private int count;

    // Número de jogadores que já responderam
    private int answered = 0;

    // Indica se o tempo expirou à força
    private boolean forcedExpired = false;

    public ModifiedCountdownLatch(int bonusFactor, int bonusCount, int waitPeriod, int count) {
        this.bonusFactor = bonusFactor;
        this.bonusCount = bonusCount;
        this.waitPeriod = waitPeriod;
        this.count = count;
    }

    
    /**
     * Regista uma resposta.
     * Também calcula se o jogador recebe bónus.
     */
    
    public synchronized int countdown() {
    	
    	// Os primeiros jogadores recebem bónus
        int factor = (answered < bonusCount) ? bonusFactor : 1;
        answered++;
        if (count > 0) {
            count--;
            
            // Se todos responderam, desbloqueia
            if (count == 0) {
                notifyAll();
            }
        }
        return factor;
    }

    
    /**
     * Espera até:
     * - todos responderem
     * OU
     * - o tempo terminar
     */
    
    public synchronized void await() throws InterruptedException {
        long deadline = System.currentTimeMillis() + waitPeriod;
        while (count > 0 && !forcedExpired) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) break;
            wait(remaining);
        }
    }

    // Força o fim da espera
    public synchronized void forceExpire() {
        this.forcedExpired = true;
        notifyAll();
    }
}