package pt.isKahoot.server;

import pt.isKahoot.shared.*;

/**
 * Thread responsável pela comunicação com um cliente.
 *
 * Responsabilidades:
 *  - gerir a ligação socket do jogador
 *  - processar mensagens recebidas
 *  - enviar atualizações do servidor
 *  - coordenar respostas individuais e de equipa
 *
 * Cada jogador ligado ao servidor possui uma instância desta classe.
 */


import java.io.*;
import java.net.Socket;

public class DealWithClient implements Runnable {
    private final Socket socket;
    private final Server server;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private String username;
    private String gameCode;
    private String teamCode;
    private GameState gameState;
    private volatile boolean running = true;

    private volatile ModifiedCountdownLatch indLatch;
    private volatile TeamBarrier tBarrier;
    private volatile ModifiedCountdownLatch globalRoundLatch;
    private volatile boolean isTeamRoundCurrent = false;
    private volatile boolean ready = false;

    public DealWithClient(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    // Estruturas de sincronização da ronda atual
    
    public void setLatches(ModifiedCountdownLatch indLatch, ModifiedCountdownLatch globalRoundLatch) {
        this.indLatch = indLatch;
        this.globalRoundLatch = globalRoundLatch;
        this.isTeamRoundCurrent = false;
    }

    public void setBarrier(TeamBarrier tBarrier, ModifiedCountdownLatch globalRoundLatch) {
        this.tBarrier = tBarrier;
        this.globalRoundLatch = globalRoundLatch;
        this.isTeamRoundCurrent = true;
    }
    
    public boolean isReady() {
        return ready;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            in = new ObjectInputStream(socket.getInputStream());

            if (!handleJoinProcess()) return;

            send(Message.waiting());

            /*
             * Todos os jogadores sincronizam aqui.
             */
            gameState.getStartBarrier().await();

            /*
             * Cliente está pronto
             * para receber perguntas.
             */
            ready = true;
            
            /*
             * Apenas UM jogador lança o jogo
             * após a barreira abrir.
             */
            if (gameState.allPlayersJoined()) {

                server.launchGameHandler(gameState);
            }
            
            /*
             * Ciclo principal de receção de mensagens do cliente.
             * Mantém-se ativo enquanto a ligação estiver aberta.
             */

            while (running && !socket.isClosed()) {

                try {

                    Message msg = (Message) in.readObject();

                    if (msg == null) break;

                    switch (msg.getType()) {

                        case ANSWER:
                            processIncomingAnswer(msg.getAnswer());
                            break;

                        default:
                            break;
                    }

                } catch (EOFException e) {
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    // Aguarda mensagem inicial de entrada no jogo
    
    private boolean handleJoinProcess() throws Exception {
        Message msg = (Message) in.readObject();
        if (msg == null || msg.getType() != Message.Type.JOIN) return false;

        username = msg.getUsername();
        gameCode = msg.getGameCode();
        teamCode = msg.getTeamCode();

        if (!server.registerUsername(username)) {
            send(Message.joinFail("Username em uso no servidor."));
            return false;
        }

        gameState = server.getGame(gameCode);
        if (gameState == null) {
            server.releaseUsername(username);
            send(Message.joinFail("Código de jogo inválido."));
            return false;
        }

        GameState.JoinResult res = gameState.registerPlayer(username, teamCode, this);
        if (res != GameState.JoinResult.OK) {
            server.releaseUsername(username);
            send(Message.joinFail("Falha ao entrar: " + res.name()));
            return false;
        }

        send(Message.joinOk());

        return true;
    }

    private void processIncomingAnswer(int answer) {

        gameState.recordAnswer(username, answer, 1);

        try {

            if (isTeamRoundCurrent) {

                if (tBarrier != null) {
                    tBarrier.await();
                }

            } else {

                if (indLatch != null) {
                    indLatch.countdown();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (globalRoundLatch != null) {
            globalRoundLatch.countdown();
        }
    }

    public synchronized void send(Message message) {
        try {
            out.writeObject(message);
            out.flush();
            out.reset();
        } catch (IOException e) {
            running = false;
        }
    }

    public void stopRunning() {
        this.running = false;
        close();
    }

    private void close() {
        try { server.releaseUsername(username); socket.close(); } catch (Exception ignored) {}
    }
}