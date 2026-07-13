package pt.isKahoot.shared;

/**
 * Barreira genérica para sincronizar
 * o arranque do jogo.
 *
 * O jogo apenas começa quando todos
 * os jogadores previstos tiverem entrado.
 */
public class GameStartBarrier {

    // Número esperado de jogadores
    private final int parties;

    // Quantos já chegaram à barreira
    private int arrived = 0;

    // Indica se a barreira abriu
    private boolean released = false;

    public GameStartBarrier(int parties) {
        this.parties = parties;
    }

    /**
     * Cada jogador chama await().
     *
     * O último jogador:
     *  - abre a barreira
     *  - desbloqueia todos
     */
    public synchronized void await() throws InterruptedException {

        if (released) return;

        arrived++;

        // Último jogador chegou
        if (arrived >= parties) {

            released = true;

            notifyAll();

            return;
        }

        // Espera até todos chegarem
        while (!released) {
            wait();
        }
    }
}