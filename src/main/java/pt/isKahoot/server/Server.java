package pt.isKahoot.server;

import pt.isKahoot.shared.Question;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servidor principal do sistema IsKahoot.
 *
 * Responsabilidades:
 *  - aceitar ligações de clientes
 *  - gerir jogos ativos
 *  - lançar GameHandlers
 *  - disponibilizar interface textual administrativa
 */

public class Server {
    private static final int PORT = 12345;
    private final Map<String, GameState> activeGames = new ConcurrentHashMap<>();
    private final Set<String> launchedGames = ConcurrentHashMap.newKeySet();
    private final Set<String> activeUsernames = ConcurrentHashMap.newKeySet();
    private final ThreadPool pool = new ThreadPool(5);

    public static void main(String[] args) {

        System.out.println("=== IsKahoot Server TUI ===");
        System.out.println("Commands:");
        System.out.println("  new <numTeams> <playersPerTeam> <numQuestions>");
        System.out.println("  list");
        System.out.println("  quit");

        Server server = new Server();

        server.start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[Server] Listening on port " + PORT);

         // Thread dedicada à interface textual do servidor
            
            Thread tui = new Thread(this::runTUI);
            tui.setDaemon(true);
            tui.start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                DealWithClient handler = new DealWithClient(clientSocket, this);
                Thread t = new Thread(handler);
                t.setDaemon(true);
                t.start();
            }
        } catch (IOException e) {
            System.err.println("Erro no servidor: " + e.getMessage());
        }
    }

    /*
     * Interface textual administrativa do servidor.
     *
     * Comandos:
     *  - new
     *  - list
     *  - quit
     */
    
    private void runTUI() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;
            String[] tokens = input.split("\\s+");

            switch (tokens[0].toLowerCase()) {
                case "new" -> handleNewGame(tokens);
                case "list" -> handleListGames();
                case "quit" -> System.exit(0);
                default -> System.out.println("Comando desconhecido (new / list / quit).");
            }
        }
    }

    private void handleNewGame(String[] tokens) {
        if (tokens.length != 4) {
            System.out.println("Uso: new <numTeams> <playersPerTeam> <numQuestions>");
            return;
        }
        try {
            int nt = Integer.parseInt(tokens[1]);
            int ppt = Integer.parseInt(tokens[2]);
            int nq = Integer.parseInt(tokens[3]);

            List<Question> poolQuestions = QuestionLoader.loadDefault();
            if (poolQuestions.isEmpty()) {
                System.out.println("Nenhuma pergunta carregada.");
                return;
            }
            if (nq > poolQuestions.size()) nq = poolQuestions.size();
            List<Question> selected = new ArrayList<>(poolQuestions.subList(0, nq));

            String code = generateCode();
            GameState game = new GameState(code, nt, ppt, selected);
            activeGames.put(code, game);
            System.out.println("[Server] Game created! Code: " + code + " | Teams: " + nt + " | Players/team: " + ppt + " | Questions: " + nq);
        } catch (NumberFormatException e) {
            System.out.println("Argumentos inválidos.");
        }
    }

    private void handleListGames() {
        if (activeGames.isEmpty()) {
            System.out.println("Nenhum jogo ativo.");
            return;
        }
        activeGames.forEach((code, game) -> {
            System.out.printf("Jogo %s | %d/%d jogadores | Placar: %s\n",
                code, game.currentPlayerCount(), game.totalExpectedPlayers(), game.getScores());
        });
    }

    public GameState getGame(String code) { return activeGames.get(code); }
    public boolean registerUsername(String u) { return activeUsernames.add(u); }
    public void releaseUsername(String u) { if (u != null) activeUsernames.remove(u); }

    public void launchGameHandler(GameState state) {
        if (launchedGames.add(state.getGameCode())) {
            pool.submit(new GameHandler(state));
        }
    }

    /**
     * Gera um código único de jogo.
     */
    
    private String generateCode() {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random r = new Random();
        String c;
        do {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
            c = sb.toString();
        } while (activeGames.containsKey(c));
        return c;
    }
}