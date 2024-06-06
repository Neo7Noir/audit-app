package com.example.auditapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AuditApp extends Application {

    private List<Question> questions;
    private Map<String, List<Question>> chapters;
    private List<String> chapterNames;
    private int currentChapterIndex = 0;
    private int totalScore = 0;

    @Override
    public void start(Stage primaryStage) {
        questions = CSVReaderUtil.readQuestionsFromCSV("src/main/resources/questionnaire/chestionar.csv");

        chapters = questions.stream().collect(Collectors.groupingBy(Question::getChapter));
        chapterNames = new ArrayList<>(chapters.keySet());

        primaryStage.setTitle("Audit Quiz");
        VBox root = new VBox(10);

        Label chapterLabel = new Label();
        Label questionLabel = new Label();
        ToggleGroup optionsGroup = new ToggleGroup();
        VBox optionsBox = new VBox(5);
        Button nextButton = new Button("Next");

        nextButton.setOnAction(event -> {
            if (areAllQuestionsAnswered(optionsGroup)) {
                updateTotalScore(optionsGroup);
                currentChapterIndex++;
                if (currentChapterIndex < questions.size()) {
                    displayChapter(chapterLabel, questionLabel, optionsBox, optionsGroup);
                } else {
                    showResults(primaryStage);
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Please answer all questions in the current chapter before proceeding.");
                alert.showAndWait();
            }
        });

        root.getChildren().addAll(chapterLabel, questionLabel, optionsBox, nextButton);
        displayChapter(chapterLabel, questionLabel, optionsBox, optionsGroup);

        Scene scene = new Scene(root, 400, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void displayChapter(Label chapterLabel, Label questionLabel, VBox optionsBox, ToggleGroup optionsGroup) {
        String currentChapter = chapterNames.get(currentChapterIndex);
        List<Question> chapterQuestions = chapters.get(currentChapter);

        chapterLabel.setText("Chapter: " + currentChapter);
        displayQuestion(chapterQuestions.get(0), questionLabel, optionsBox, optionsGroup);

        // Clear previous answers
        optionsGroup.getToggles().forEach(toggle -> toggle.setSelected(false));
    }

    private void displayQuestion(Question question, Label questionLabel, VBox optionsBox, ToggleGroup optionsGroup) {
        questionLabel.setText(question.getQuestionText());
        optionsBox.getChildren().clear();
        optionsGroup.getToggles().clear();

        for (int i = 0; i < question.getOptions().size(); i++) {
            RadioButton optionButton = new RadioButton(question.getOptions().get(i));
            optionButton.setUserData(question.getScores().get(i));
            optionButton.setToggleGroup(optionsGroup);
            optionsBox.getChildren().add(optionButton);
        }
    }

    private boolean areAllQuestionsAnswered(ToggleGroup optionsGroup) {
        return optionsGroup.getSelectedToggle() != null;
    }

    private void updateTotalScore(ToggleGroup optionsGroup) {
        RadioButton selectedOption = (RadioButton) optionsGroup.getSelectedToggle();
        if (selectedOption != null) {
            int selectedScore = (int) selectedOption.getUserData();
            totalScore += selectedScore;
        }
    }

    private void showResults(Stage stage) {
        VBox resultsBox = new VBox(10);
        Label resultsLabel = new Label("Total Score: " + totalScore);
        resultsBox.getChildren().add(resultsLabel);

        Scene resultsScene = new Scene(resultsBox, 400, 300);
        stage.setScene(resultsScene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}