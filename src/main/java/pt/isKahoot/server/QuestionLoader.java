package pt.isKahoot.server;

import com.google.gson.Gson;
import pt.isKahoot.shared.GameConfig;
import pt.isKahoot.shared.Question;

import java.io.*;
import java.util.Collections;
import java.util.List;

/**
 * Responsável pelo carregamento das perguntas
 * a partir do ficheiro JSON.
 */

public class QuestionLoader {

    private static final String DEFAULT_FILE = "questions.json";

    public static List<Question> load(String path) {
        try (Reader reader = new FileReader(path)) {
            Gson gson = new Gson();
            GameConfig config = gson.fromJson(reader, GameConfig.class);
            List<Question> questions = config.getQuestions();
            Collections.shuffle(questions);
            return questions;
        } catch (IOException e) {
            System.err.println("[QuestionLoader] Failed to load questions from: " + path);
            e.printStackTrace();
            return List.of();
        }
    }

    public static List<Question> loadDefault() {
        return load(DEFAULT_FILE);
    }
}
