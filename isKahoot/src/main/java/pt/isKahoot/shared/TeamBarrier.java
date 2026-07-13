package pt.isKahoot.shared;

/**
 * Barreira de sincronização usada para rondas em equipa.
 *
 * Todos os jogadores da equipa têm de responder
 * antes da ronda continuar.
 */

public class TeamBarrier {
	
	// Número de jogadores da equipa
    private final int parties;

    // Tempo máximo de espera
    private final int waitPeriod;

    // Ação opcional executada quando a barreira termina
    private final Runnable barrierAction;

    // Jogadores ainda em falta
    private int count;

    // Indica se a barreira já foi libertada
    private boolean released = false;

    public TeamBarrier(int parties, int waitPeriod, Runnable barrierAction) {
        this.parties = parties;
        this.waitPeriod = waitPeriod;
        this.barrierAction = barrierAction;
        this.count = parties;
    }

    
    /**
     * Espera até todos os membros responderem
     * ou até o tempo terminar.
     */
    
    public synchronized void await() throws InterruptedException {
        if (released) return;
        count--;
        
        // Todos responderam
        if (count <= 0) {
            if (barrierAction != null) barrierAction.run();
            released = true;
            notifyAll();
            return;
        }

        long deadline = System.currentTimeMillis() + waitPeriod;
        while (!released) {
            long remaining = deadline - System.currentTimeMillis();
            
            // Tempo expirou
            if (remaining <= 0) {
                if (barrierAction != null) barrierAction.run();
                released = true;
                notifyAll();
                break;
            }
            wait(remaining);
        }
    }

    //Liberta manualmente a barreira
    
    public synchronized void forceRelease() {
        if (!released) {
            if (barrierAction != null) barrierAction.run();
            released = true;
            notifyAll();
        }
    }
}