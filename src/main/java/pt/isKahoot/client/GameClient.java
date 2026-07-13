package pt.isKahoot.client;

import pt.isKahoot.shared.Message;

import java.io.*;
import java.net.Socket;

/**
 * Classe responsável pela comunicação de rede do cliente.
 *
 * Responsabilidades:
 *  - estabelecer ligação ao servidor
 *  - enviar respostas do jogador
 *  - receber mensagens do servidor
 *  - encaminhar atualizações para a interface gráfica
 *
 * Executa numa thread dedicada à comunicação.
 */
public class GameClient implements Runnable {

    private final String host;
    private final int port;
    private final String gameCode;
    private final String teamCode;
    private final String username;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    private GameGUI gui;
    private volatile boolean connected = false;

    public GameClient(String host, int port, String gameCode, String teamCode, String username) {
        this.host     = host;
        this.port     = port;
        this.gameCode = gameCode;
        this.teamCode = teamCode;
        this.username = username;
    }

    public void setGui(GameGUI gui) {
        this.gui = gui;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(host, port)) {

        	out = new ObjectOutputStream(socket.getOutputStream());
        	in  = new ObjectInputStream(socket.getInputStream());

        	connected = true;

            // Envia pedido de entrada no jogo
            send(Message.join(gameCode, teamCode, username));

            /*
             * Ciclo principal de receção de mensagens.
             * O cliente permanece à escuta até ao final do jogo
             * ou ao encerramento da ligação.
             */
            while (true) {

                Message msg = (Message) in.readObject();

                handleMessage(msg);

                if (msg.getType() == Message.Type.GAME_OVER) {
                    break;
                }
            }

        } catch (IOException | ClassNotFoundException e) {

            if (gui != null) {
                gui.showError("Disconnected: " + e.getMessage());
            }
        }
    }

    /**
     * Processa mensagens recebidas do servidor
     * e atualiza a interface gráfica.
     */
    private void handleMessage(Message msg) {

        /*
         * Espera até a GUI estar pronta.
         */
        while (gui == null) {

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        switch (msg.getType()) {

            case JOIN_OK:
            case WAITING:
                gui.showWaiting();
                break;

            case JOIN_FAIL:
                gui.showError(msg.getText());
                break;

            case QUESTION:
                gui.showQuestion(msg.getQuestionPayload());
                break;

            case ROUND_RESULT:
                gui.showScoreboard(msg.getScorePayload(), false);
                break;

            case GAME_OVER:
                gui.showScoreboard(msg.getScorePayload(), true);
                break;

            default:
                break;
        }
    }

    /**
     * Envia a resposta escolhida pelo jogador.
     */
    public synchronized void sendAnswer(int answer) {
        send(Message.answer(answer));
    }

    /**
     * Envia uma mensagem para o servidor.
     *
     * O método é sincronizado para evitar acessos concorrentes
     * ao ObjectOutputStream.
     */
    private void send(Message msg) {

        // Log de debug para acompanhamento das mensagens enviadas
        System.out.println("[CLIENT] Sending: " + msg.getType());

        try {

            if (out == null) {
                System.out.println("[CLIENT] ERROR: out stream is null");
                return;
            }

            out.writeObject(msg);
            out.flush();
            out.reset();

        } catch (IOException e) {

            e.printStackTrace();

            if (gui != null) {
                gui.showError("Send error: " + e.getMessage());
            }
        }
    }

    /**
     * Indica se a ligação ao servidor já foi estabelecida.
     */
    public boolean isConnected() {
        return connected;
    }
}
