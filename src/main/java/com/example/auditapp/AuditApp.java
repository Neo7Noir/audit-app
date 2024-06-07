package com.example.auditapp;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.*;
import java.util.stream.Collectors;

public class AuditApp extends Application {

    // Existing fields
    private List<Question> questions;
    private Map<String, List<Question>> chapters;
    private List<String> chapterNames;
    private int currentChapterIndex = 0;
    private int currentQuestionIndex = 0;
    private int totalScore = 0;
    private Map<String, Integer> chapterScores = new HashMap<>();

    // Best Practices fields
    private List<BestPractice> bestPractices;
    private Map<String, List<BestPractice>> bestPracticesByChapter;
    private Map<String, Integer> chapterThresholds;

    @Override
    public void start(Stage primaryStage) {
        questions = CSVReaderUtil.readQuestionsFromCSV("src/main/resources/questionnaire/chestionar.csv");
        bestPractices = CSVReaderUtil.readSuggestionFromCSV("src/main/resources/questionnaire/best_practices.csv");

        chapters = questions.stream().collect(Collectors.groupingBy(Question::getChapter));
        chapterNames = new ArrayList<>(chapters.keySet());

        bestPracticesByChapter = bestPractices.stream().collect(Collectors.groupingBy(BestPractice::getChapter));

        primaryStage.setTitle("Audit App");

        VBox root = new VBox(10);

        // Buttons for switching views
        Button quizButton = new Button("Start Quiz");
//        Button bestPracticesButton = new Button("View Best Practices");

        quizButton.setOnAction(event -> startQuiz(primaryStage));
//        bestPracticesButton.setOnAction(event -> showBestPractices(primaryStage));

        root.getChildren().addAll(quizButton);

        Scene scene = new Scene(root, 600, 300);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void startQuiz(Stage primaryStage) {
        VBox root = new VBox(10);

        Label chapterLabel = new Label();
        Label questionLabel = new Label();
        ToggleGroup optionsGroup = new ToggleGroup();
        VBox optionsBox = new VBox(5);
        Button nextButton = new Button("Next");

        nextButton.setOnAction(event -> {
            if (areAllQuestionsAnswered(optionsGroup)) {
                updateTotalScore(optionsGroup);
                currentQuestionIndex++;
                List<Question> chapterQuestions = chapters.get(chapterNames.get(currentChapterIndex));
                if (currentQuestionIndex < chapterQuestions.size()) {
                    displayQuestion(chapterQuestions.get(currentQuestionIndex), questionLabel, optionsBox, optionsGroup);
                } else {
                    System.out.println("Chapter " + chapterNames.get(currentChapterIndex) + " completed.");
                    double percentage = calculatePercentage(chapterNames.get(currentChapterIndex));
                    System.out.println("Percentage obtained: " + percentage + "%");
                    List<BestPractice> bestPractices1 = getSuggestedBestPractices(bestPractices, percentage);
                    for (BestPractice practice: bestPractices1) {
                        System.out.println(practice.toString());
                    }
                    currentQuestionIndex = 0;
                    currentChapterIndex++;
                    if (currentChapterIndex < chapterNames.size()) {
                        displayChapter(chapterLabel, questionLabel, optionsBox, optionsGroup);
                    } else {
                        showResults(primaryStage);
                    }
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Please answer all questions in the current chapter before proceeding.");
                alert.showAndWait();
            }
        });

        root.getChildren().addAll(chapterLabel, questionLabel, optionsBox, nextButton);
        displayChapter(chapterLabel, questionLabel, optionsBox, optionsGroup);

        Scene scene = new Scene(root, 600, 300);
        primaryStage.setScene(scene);
    }

    private void displayChapter(Label chapterLabel, Label questionLabel, VBox optionsBox, ToggleGroup optionsGroup) {
        String currentChapter = chapterNames.get(currentChapterIndex);
        List<Question> chapterQuestions = chapters.get(currentChapter);

        chapterLabel.setText("Chapter: " + currentChapter);
        displayQuestion(chapterQuestions.get(currentQuestionIndex), questionLabel, optionsBox, optionsGroup);
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

        // Clear previous answers
        optionsGroup.getToggles().forEach(toggle -> toggle.setSelected(false));
    }

    private boolean areAllQuestionsAnswered(ToggleGroup optionsGroup) {
        return optionsGroup.getSelectedToggle() != null;
    }

    private void updateTotalScore(ToggleGroup optionsGroup) {
        RadioButton selectedOption = (RadioButton) optionsGroup.getSelectedToggle();
        if (selectedOption != null) {
            int selectedScore = (int) selectedOption.getUserData();
            totalScore += selectedScore;

            String currentChapter = chapterNames.get(currentChapterIndex);
            chapterScores.put(currentChapter, chapterScores.getOrDefault(currentChapter, 0) + selectedScore);
        }
    }

    private void showResults(Stage stage) {
        VBox resultsBox = new VBox(10);
        Label resultsLabel = new Label("Total Score: " + totalScore);
        resultsBox.getChildren().add(resultsLabel);

        for (String chapter : chapterScores.keySet()) {
            Label chapterScoreLabel = new Label("Chapter " + chapter + ": " + chapterScores.get(chapter));
            resultsBox.getChildren().add(chapterScoreLabel);
        }

        // Add a button to show assigned best practices
        Button showBestPracticesButton = new Button("Show Assigned Best Practices");
        showBestPracticesButton.setOnAction(event -> showAssignedBestPractices(stage));

        resultsBox.getChildren().add(showBestPracticesButton);

        Scene resultsScene = new Scene(resultsBox, 400, 300);
        stage.setScene(resultsScene);
    }

    private void showAssignedBestPractices(Stage stage) {
        VBox assignedBestPracticesBox = new VBox(10);
        Label titleLabel = new Label("Assigned Best Practices");
        assignedBestPracticesBox.getChildren().add(titleLabel);

        for (String chapter : chapterScores.keySet()) {
            List<BestPractice> chapterBestPractices = bestPracticesByChapter.getOrDefault(chapter, new ArrayList<>());
            double scorePercentage = calculatePercentage(chapter);

            List<BestPractice> suggestedPractices = getSuggestedBestPractices(chapterBestPractices, scorePercentage);
            for (BestPractice bestPractice : suggestedPractices) {
                Label bestPracticeLabel = new Label("Chapter: " + chapter + " - " + bestPractice.getBestPractice());
                assignedBestPracticesBox.getChildren().add(bestPracticeLabel);
            }
        }

        Button backButton = new Button("Back to Results");
        backButton.setOnAction(event -> showResults(stage));
        assignedBestPracticesBox.getChildren().add(backButton);

        Scene bestPracticesScene = new Scene(assignedBestPracticesBox, 600, 400);
        stage.setScene(bestPracticesScene);
    }

    private List<BestPractice> getSuggestedBestPractices(List<BestPractice> bestPractices, double scorePercentage) {
        if (scorePercentage <= 25) {
            return bestPractices.subList(0, 1); // Suggest first best practice
        } else if (scorePercentage <= 50) {
            return bestPractices.subList(0, Math.min(2, bestPractices.size())); // Suggest first two best practices
        } else if (scorePercentage <= 75) {
            return bestPractices.subList(0, Math.min(3, bestPractices.size())); // Suggest all best practices
        } else {
            return bestPractices.subList(0, Math.min(4, bestPractices.size()));
        }
    }

    private void showBestPractices(Stage primaryStage) {
        TableView<BestPractice> tableView = new TableView<>();

        TableColumn<BestPractice, String> chapterColumn = new TableColumn<>("Chapter");
        chapterColumn.setCellValueFactory(new PropertyValueFactory<>("chapter"));

        TableColumn<BestPractice, String> categoryColumn = new TableColumn<>("Category");
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<BestPractice, String> bestPracticeColumn = new TableColumn<>("Best Practice");
        bestPracticeColumn.setCellValueFactory(new PropertyValueFactory<>("bestPractice"));


        tableView.getColumns().addAll(chapterColumn, categoryColumn, bestPracticeColumn);
        tableView.setItems(FXCollections.observableArrayList(bestPractices));

        Button backButton = new Button("Back to Main Menu");
        backButton.setOnAction(event -> start(primaryStage));

        VBox root = new VBox(10);
        root.getChildren().addAll(tableView, backButton);

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setScene(scene);
    }

    private long calculatePercentage(String chapterName) {
        int score = chapterScores.getOrDefault(chapterName, 0);
        int totalPossibleScore = chapters.get(chapterName).stream().mapToInt(q -> Collections.max(q.getScores())).sum();
        return Math.round(((double) score / totalPossibleScore) * 100);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
