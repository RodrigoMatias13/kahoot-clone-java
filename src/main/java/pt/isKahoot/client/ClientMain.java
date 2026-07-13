package pt.isKahoot.client;

import javax.swing.SwingUtilities;

/**
 * Classe principal do cliente IsKahoot.
 *
 * Responsabilidades:
 *  - validar os argumentos da linha de comandos
 *  - criar o cliente de rede
 *  - iniciar a thread de comunicação
 *  - lançar a interface gráfica Swing
 *
 * Utilização:
 * java -jar client.jar <IP> <PORTO> <GameCode> <TeamCode> <Username>
 */
public class ClientMain {

	public static void main(String[] args) {

	    // Verifica se todos os argumentos obrigatórios foram fornecidos
	    if (args.length != 5) {
	        System.err.println(
	                "Usage: java ClientMain <IP> <PORT> <GameCode> <TeamCode> <Username>"
	        );
	        System.exit(1);
	    }

	    String host     = args[0];
	    int port        = Integer.parseInt(args[1]);
	    String gameCode = args[2];
	    String teamCode = args[3];
	    String username = args[4];

	    // Criação do cliente responsável pela comunicação com o servidor
	    GameClient client =
	            new GameClient(
	                    host,
	                    port,
	                    gameCode,
	                    teamCode,
	                    username
	            );

	    // Thread dedicada à comunicação de rede
	    Thread networkThread = new Thread(client);

	    // A thread termina automaticamente quando a aplicação fecha
	    networkThread.setDaemon(true);

	    networkThread.start();

	    /*
	     * Aguarda até a ligação ao servidor estar estabelecida
	     * antes de iniciar a interface gráfica.
	     */
	    while (!client.isConnected()) {

	        try {
	            Thread.sleep(50);
	        } catch (InterruptedException e) {
	            Thread.currentThread().interrupt();
	        }
	    }

	    /*
	     * Todas as operações gráficas Swing devem ser executadas
	     * na Event Dispatch Thread.
	     */
	    SwingUtilities.invokeLater(() -> {

	        GameGUI gui =
	                new GameGUI(
	                        client,
	                        username,
	                        teamCode
	                );

	        client.setGui(gui);
	    });
	}
}