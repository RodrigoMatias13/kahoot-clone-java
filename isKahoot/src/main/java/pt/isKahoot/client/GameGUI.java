package pt.isKahoot.client;

import pt.isKahoot.shared.Message;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Interface gráfica.
 *
 * Responsável por apresentar:
 *  - ecrã de espera
 *  - perguntas e opções de resposta
 *  - temporizador da ronda
 *  - classificação intermédia
 *  - classificação final
 *
 * Utiliza CardLayout para alternar entre diferentes ecrãs.
 */
public class GameGUI extends JFrame {

    private static final long serialVersionUID = 1L;
	private final GameClient client;
    private final String username;
    private final String teamCode;

    // Painéis principais
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);

    // Painel de espera
    private final JLabel waitingLabel = new JLabel("Waiting for other players...", SwingConstants.CENTER);

    // Painel de perguntas
    private final JLabel questionLabel   = new JLabel("", SwingConstants.CENTER);
    private final JLabel timerLabel      = new JLabel("", SwingConstants.CENTER);
    private final JLabel roundTypeLabel  = new JLabel("", SwingConstants.CENTER);
    private final JPanel optionsPanel    = new JPanel(new GridLayout(2, 2, 10, 10));
    private final JButton[] optionButtons = new JButton[4];

    // Painel de pontuação
    private final JPanel scorePanel      = new JPanel();
    private final JLabel roundPointsLabel = new JLabel("", SwingConstants.CENTER);
    private final JTextArea scoreArea    = new JTextArea();

    // Temporizador
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> timerFuture;
    private int secondsLeft;

    public GameGUI(GameClient client, String username, String teamCode) {
        super("IsKahoot — " + username + " [" + teamCode + "]");
        this.client   = client;
        this.username = username;
        this.teamCode = teamCode;
        buildUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ----------------------------------------------------------------
    // UI Construction
    // ----------------------------------------------------------------

    private void buildUI() {
        mainPanel.add(buildWaitingPanel(), "WAITING");
        mainPanel.add(buildQuestionPanel(), "QUESTION");
        mainPanel.add(buildScorePanel(), "SCORE");
        mainPanel.add(buildErrorPanel(), "ERROR");
        add(mainPanel);
        cardLayout.show(mainPanel, "WAITING");
    }

    private JPanel buildWaitingPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(46, 7, 83));
        waitingLabel.setFont(new Font("Arial", Font.BOLD, 22));
        waitingLabel.setForeground(Color.WHITE);
        p.add(waitingLabel, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildQuestionPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(new Color(46, 7, 83));
        p.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top bar: round type + timer
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(46, 7, 83));
        roundTypeLabel.setFont(new Font("Arial", Font.ITALIC, 14));
        roundTypeLabel.setForeground(new Color(200, 200, 255));
        timerLabel.setFont(new Font("Arial", Font.BOLD, 28));
        timerLabel.setForeground(Color.YELLOW);
        topBar.add(roundTypeLabel, BorderLayout.WEST);
        topBar.add(timerLabel, BorderLayout.EAST);
        p.add(topBar, BorderLayout.NORTH);

        // Question text
        questionLabel.setFont(new Font("Arial", Font.BOLD, 20));
        questionLabel.setForeground(Color.WHITE);
        questionLabel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        p.add(questionLabel, BorderLayout.CENTER);

        // Option buttons
        Color[] colours = {
            new Color(226, 27, 60),
            new Color(19, 104, 206),
            new Color(253, 174, 0),
            new Color(38, 137, 12)
        };
        String[] shapes = {"▲ ", "◆ ", "● ", "■ "};
        for (int i = 0; i < 4; i++) {
            int idx = i;
            optionButtons[i] = new JButton();
            optionButtons[i].setFont(new Font("Arial", Font.BOLD, 16));
            optionButtons[i].setBackground(colours[i]);
            optionButtons[i].setForeground(Color.WHITE);
            optionButtons[i].setFocusPainted(false);
            optionButtons[i].setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            final int answer = idx;
            optionButtons[i].addActionListener(e -> submitAnswer(answer));
            optionsPanel.add(optionButtons[i]);
        }
        optionsPanel.setBackground(new Color(46, 7, 83));
        p.add(optionsPanel, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildScorePanel() {
        scorePanel.setLayout(new BorderLayout(10, 10));
        scorePanel.setBackground(new Color(46, 7, 83));
        scorePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        roundPointsLabel.setFont(new Font("Arial", Font.BOLD, 26));
        roundPointsLabel.setForeground(Color.YELLOW);
        scorePanel.add(roundPointsLabel, BorderLayout.NORTH);

        scoreArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        scoreArea.setBackground(new Color(30, 0, 60));
        scoreArea.setForeground(Color.WHITE);
        scoreArea.setEditable(false);
        scorePanel.add(new JScrollPane(scoreArea), BorderLayout.CENTER);
        return scorePanel;
    }

    private JPanel buildErrorPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(180, 0, 0));
        JLabel label = new JLabel("Connection error", SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 20));
        label.setForeground(Color.WHITE);
        p.add(label, BorderLayout.CENTER);
        return p;
    }

    /*
     * Todas as atualizações da interface gráfica
     * são executadas na Event Dispatch Thread
     * para garantir segurança no acesso ao Swing.
     */

    public void showWaiting() {
        SwingUtilities.invokeLater(() -> {
            waitingLabel.setText("Waiting for other players to join...");
            cardLayout.show(mainPanel, "WAITING");
        });
    }

    public void showQuestion(Message.QuestionPayload payload) {
        SwingUtilities.invokeLater(() -> {
            stopTimer();
            questionLabel.setText("<html><div style='text-align:center'>" + payload.questionText + "</div></html>");
            roundTypeLabel.setText(payload.isTeamRound ? "👥 Team Round" : "👤 Individual Round");

            List<String> opts = payload.options;
            String[] shapes = {"▲ ", "◆ ", "● ", "■ "};
            for (int i = 0; i < optionButtons.length; i++) {
                String text = (i < opts.size()) ? shapes[i] + opts.get(i) : "";
                optionButtons[i].setText(text);
                optionButtons[i].setEnabled(true);
            }

            startTimer(payload.timeLimitSeconds);
            cardLayout.show(mainPanel, "QUESTION");
        });
    }

    public void showScoreboard(Message.ScorePayload payload, boolean isFinal) {
        SwingUtilities.invokeLater(() -> {
            stopTimer();
            String prefix = isFinal ? "🏆 Final Scores" : "Round " + payload.questionNumber + "/" + payload.totalQuestions;
            roundPointsLabel.setText(prefix + "  |  Your team earned: " + payload.roundPoints + " pts");

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-20s %s%n", "Team", "Score"));
            sb.append("─".repeat(30)).append("\n");

            payload.scores.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .forEach(e -> {
                        String marker = e.getKey().equals(teamCode) ? " ◄ you" : "";
                        sb.append(String.format("%-20s %d%s%n", e.getKey(), e.getValue(), marker));
                    });

            scoreArea.setText(sb.toString());
            cardLayout.show(mainPanel, "SCORE");
        });
    }

    public void showError(String msg) {
        SwingUtilities.invokeLater(() -> {
            waitingLabel.setText("Error: " + msg);
            cardLayout.show(mainPanel, "WAITING");
        });
    }

    /*
     * Temporizador atualizado a cada segundo.
     * O contador termina automaticamente quando
     * o tempo chega a zero.
     */

    private void startTimer(int seconds) {
        secondsLeft = seconds;
        timerLabel.setText(String.valueOf(secondsLeft));
        timerFuture = scheduler.scheduleAtFixedRate(() -> {
            secondsLeft--;
            SwingUtilities.invokeLater(() -> {
                timerLabel.setText(String.valueOf(Math.max(0, secondsLeft)));
                if (secondsLeft <= 5) timerLabel.setForeground(Color.RED);
                else timerLabel.setForeground(Color.YELLOW);
            });
            if (secondsLeft <= 0) stopTimer();
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void stopTimer() {
        if (timerFuture != null && !timerFuture.isCancelled()) {
            timerFuture.cancel(false);
        }
    }

    // ----------------------------------------------------------------
    // Answer submission
    // ----------------------------------------------------------------

    private void submitAnswer(int answer) {
        // Impede múltiplos envios de resposta
        for (JButton btn : optionButtons) btn.setEnabled(false);
        client.sendAnswer(answer);
    }
}
