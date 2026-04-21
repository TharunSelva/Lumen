package com.energyapp.controller;

import com.energyapp.db.HomeProfileRepository;
import com.energyapp.model.EnergyRecord;
import com.energyapp.model.Recommendation;
import com.energyapp.service.RecommendationFeedService;
import com.energyapp.service.SimulationService;
import com.energyapp.utils.DateTimeUtils;
import com.energyapp.utils.EnergyCalculator;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.StringConverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DashboardController {

    private static final int HOME_VISIBLE_RECOMMENDATIONS = 3;
    private static final String HOME_ID = "HOME_1";

    @FXML private VBox dashboard00VBox;
    @FXML private VBox dashboard10VBox;
    @FXML private BorderPane dashboardBorderPane;
    @FXML private Label dashboardEcoImpactCleanLabel;
    @FXML private GridPane dashboardEcoImpactDividerGridPane;
    @FXML private Label dashboardEcoImpactFossilLabel;
    @FXML private VBox dashboardEcoImpactInsightVBox;
    @FXML private PieChart dashboardEcoImpactPieChart;
    @FXML private Label dashboardEcoImpactTitleLabel;
    @FXML private VBox dashboardEcoImpactVBox;
    @FXML private Label dashboardGreetingLabel;
    @FXML private GridPane dashboardMainGridPane;
    @FXML private HBox dashboardQuickForecastBarHBox;
    @FXML private Label dashboardQuickForecastTitleLabel;
    @FXML private VBox dashboardQuickForecastVBox;
    @FXML private VBox dashboardQuickRecommendationsList;
    @FXML private StackPane dashboardQuickRecommendationsViewport;
    @FXML private VBox dashboardQuickRecContainerVBox;
    @FXML private GridPane dashboardQuickRecGridPane;
    @FXML private Label dashboardQuickRecTitleLabel;
    @FXML private VBox dashboardSidebarVBox;
    @FXML private Label dashboardUsageInsightRightLabel;
    @FXML private VBox dashboardUsageInsightRightVBox;
    @FXML private GridPane dashboardUsageInsightsGridPane;
    @FXML private ComboBox<String> zoomComboBox;
    @FXML private VBox dashboardContentVBox;
    @FXML private ScrollPane dashboardScrollPane;
    @FXML private LineChart<Number, Number> dashboardUsageLineChart;
    @FXML private NumberAxis dashboardUsageXAxis;
    @FXML private NumberAxis dashboardUsageYAxis;
    @FXML private Label dashboardUsageTitleLabel;
    @FXML private VBox dashboardWeeklyBudgetVBox;
    @FXML private Label dashboardWeeklyBudgetTitleLabel;
    @FXML private Label dashboardWeeklyBudgetSpentLabel;
    @FXML private Label dashboardWeeklyBudgetBudgetLabel;
    @FXML private Label dashboardWeeklyBudgetRemainingLabel;
    @FXML private Label dashboardWeeklyBudgetStatusLabel;
    @FXML private Label dashboardWeeklyBudgetHelperLabel;
    @FXML private ProgressBar dashboardWeeklyBudgetProgressBar;
    @FXML private Button dashboardWeeklyBudgetEditButton;
    @FXML private CostCardController weeklyCostCardController;
    @FXML private Label priceStatusText;
    @FXML private Circle priceIndicator;

    private PriceIndicatorController indicatorLogic;
    private MainLayoutController mainController;
    private SimulationService simulationService;
    private final RecommendationFeedService recommendationFeedService = RecommendationFeedService.getInstance();
    private final HomeProfileRepository homeProfileRepository = new HomeProfileRepository();
    private final XYChart.Series<Number, Number> series = new XYChart.Series<>();
    private final ChangeListener<Number> intervalCostListener = (obs, oldCost, newCost) -> {
        if (newCost != null) {
            indicatorLogic.updateIndicatorByCost(newCost.doubleValue());
        }
    };
    private final ChangeListener<Number> weeklyCostListener = (obs, oldCost, newCost) -> refreshWeeklyBudgetDisplay();
    private final ChangeListener<EnergyRecord> chartRecordListener = (obs, oldVal, newVal) -> {
        if (newVal != null) {
            Platform.runLater(() -> plotDataPoint(newVal));
        }
    };
    private final ChangeListener<EnergyRecord> dashboardRecordListener = (obs, oldRecord, newRecord) -> {
        if (newRecord != null) {
            refreshWeeklyBudgetDisplay();
            updateDashboardRecommendations(newRecord);
        }
    };

    private long windowSeconds = 60 * 60;
    private long latestEpochSec = 0;
    private boolean chartWired = false;
    private Double currentWeeklyBudgetGbp;

    @FXML
    public void initialize() {
        indicatorLogic = new PriceIndicatorController(priceStatusText, priceIndicator);
        System.out.println("DashboardController loaded successfully.");
        dashboardUsageLineChart.getData().add(series);
        setupTimeAxis(dashboardUsageXAxis);
        dashboardUsageXAxis.setLabel("Time");
        dashboardUsageYAxis.setLabel("Power (kWh)");
        dashboardUsageYAxis.setAutoRanging(true);

        dashboardUsageLineChart.setOnMouseClicked(e -> openEnergyStatistics());

        zoomComboBox.getItems().addAll("100%", "125%", "150%", "175%");
        zoomComboBox.setValue("100%");
        zoomComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                double newZoomLevel = Double.parseDouble(newVal.replace("%", "")) / 100;
                dashboardContentVBox.setScaleX(newZoomLevel);
                dashboardContentVBox.setScaleY(newZoomLevel);
            }
        });

        dashboardScrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) ->
                dashboardContentVBox.setPrefWidth(newBounds.getWidth()));

        if (dashboardQuickRecommendationsViewport != null) {
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(dashboardQuickRecommendationsViewport.widthProperty());
            clip.heightProperty().bind(dashboardQuickRecommendationsViewport.heightProperty());
            dashboardQuickRecommendationsViewport.setClip(clip);
        }

        reloadWeeklyBudgetFromRepository();
        refreshWeeklyBudgetDisplay();
    }

    private void openEnergyStatistics() {
        if (mainController != null) {
            mainController.handleShowStatistics();
        }
    }

    public void setMainController(MainLayoutController mainController) {
        this.mainController = mainController;
    }

    public void setSimService(SimulationService simulationService) {
        if (this.simulationService != null) {
            this.simulationService.currentIntervalCostProperty().removeListener(intervalCostListener);
            this.simulationService.weeklyAccumulatedCostProperty().removeListener(weeklyCostListener);
            this.simulationService.getCurrentEnergyRecord().removeListener(chartRecordListener);
            this.simulationService.getCurrentEnergyRecord().removeListener(dashboardRecordListener);
        }

        this.simulationService = simulationService;
        chartWired = false;
        initUsageMiniChart();

        EnergyRecord currentRecord = simulationService.getCurrentEnergyRecord().get();
        if (currentRecord != null) {
            double currentCost = EnergyCalculator.calculateCostInPounds(
                    currentRecord.getGridImport(),
                    currentRecord.getPricePerKwh()
            );
            indicatorLogic.updateIndicatorByCost(currentCost);
        }

        simulationService.currentIntervalCostProperty().addListener(intervalCostListener);
        simulationService.weeklyAccumulatedCostProperty().addListener(weeklyCostListener);
        simulationService.getCurrentEnergyRecord().addListener(dashboardRecordListener);

        refreshWeeklyBudgetDisplay();
        updateDashboardRecommendations(currentRecord);
    }

    private void initUsageMiniChart() {
        if (simulationService == null || chartWired) {
            return;
        }
        chartWired = true;
        series.getData().clear();
        latestEpochSec = 0;

        List<EnergyRecord> history = simulationService.getEnergyStatsSessionHistory();
        for (EnergyRecord rec : history) {
            plotDataPoint(rec);
        }

        simulationService.getCurrentEnergyRecord().addListener(chartRecordListener);
    }

    private void setupTimeAxis(NumberAxis axis) {
        axis.setAutoRanging(false);
        axis.setLabel("Time");
        axis.setForceZeroInRange(false);
        axis.setTickUnit(10 * 60);

        axis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number object) {
                return DateTimeUtils.formatTimeFromEpoch(object.longValue());
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });
    }

    private void plotDataPoint(EnergyRecord rec) {
        long x = DateTimeUtils.toEpochSeconds(rec.getTimestamp());
        double y = rec.getTotalLoadKwh();
        latestEpochSec = x;

        XYChart.Data<Number, Number> d = new XYChart.Data<>(x, y);
        series.getData().add(d);
        updateAxisBounds();
    }

    private void updateAxisBounds() {
        if (latestEpochSec == 0) {
            return;
        }

        long ub = latestEpochSec;
        long lb = ub - windowSeconds;

        dashboardUsageXAxis.setLowerBound(lb);
        dashboardUsageXAxis.setUpperBound(ub);
        trimToWindow(lb);
    }

    private void trimToWindow(long lb) {
        while (!series.getData().isEmpty()) {
            long t = series.getData().get(0).getXValue().longValue();
            if (t >= lb) {
                break;
            }
            series.getData().remove(0);
        }
    }

    private void updateDashboardRecommendations(EnergyRecord currentRecord) {
        if (currentRecord == null) {
            return;
        }

        Platform.runLater(() -> renderDashboardRecommendations(
                recommendationFeedService.updateRecommendations(currentRecord)
        ));
    }

    private void renderDashboardRecommendations(RecommendationFeedService.FeedUpdate feedUpdate) {
        if (dashboardQuickRecommendationsList == null) {
            return;
        }
        List<Recommendation> allRecommendations = feedUpdate.getVisibleRecommendations();
        List<Recommendation> recommendations = allRecommendations.size() <= HOME_VISIBLE_RECOMMENDATIONS
                ? allRecommendations
                : allRecommendations.subList(allRecommendations.size() - HOME_VISIBLE_RECOMMENDATIONS, allRecommendations.size());
        recommendations = newestFirst(recommendations);

        if (recommendations.isEmpty()) {
            dashboardQuickRecommendationsList.getChildren().clear();
            Label emptyState = new Label("No live recommendations just yet. Check back as your energy data updates.");
            emptyState.getStyleClass().add("recommendation-empty-state");
            emptyState.setWrapText(true);
            dashboardQuickRecommendationsList.getChildren().add(emptyState);
            return;
        }

        if (dashboardQuickRecommendationsList.getChildren().isEmpty() || feedUpdate.insertedCount() != 1) {
            replaceRecommendationCards(recommendations);
            return;
        }

        Recommendation insertedRecommendation = recommendations.stream()
                .filter(recommendation -> feedUpdate.isNewlyInserted(recommendation.getId()))
                .findFirst()
                .orElse(null);

        if (insertedRecommendation == null) {
            replaceRecommendationCards(recommendations);
            return;
        }

        if (dashboardQuickRecommendationsList.getChildren().size() < HOME_VISIBLE_RECOMMENDATIONS) {
            Region cardNode = createRecommendationCard(insertedRecommendation);
            animateNewTopCard(cardNode);
            return;
        }

        animatePushDownTransition(insertedRecommendation, recommendations);
    }

    private void replaceRecommendationCards(List<Recommendation> recommendations) {
        dashboardQuickRecommendationsList.getChildren().clear();
        for (int i = 0; i < recommendations.size(); i++) {
            dashboardQuickRecommendationsList.getChildren().add(createRecommendationCard(recommendations.get(i), i == 0));
        }
    }

    private Region createRecommendationCard(Recommendation recommendation) {
        return createRecommendationCard(recommendation, false);
    }

    private Region createRecommendationCard(Recommendation recommendation, boolean highlightLatest) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/energyapp/view/RecommendationCard.fxml"));
            Region cardNode = loader.load();
            RecommendationCardController controller = loader.getController();
            controller.setRecommendation(recommendation);
            controller.setOnNavigate(() -> {
                if (mainController != null) {
                    mainController.handleShowRecommendations();
                }
            });
            cardNode.getStyleClass().remove("recommendation-card-latest");
            if (highlightLatest) {
                cardNode.getStyleClass().add("recommendation-card-latest");
            }
            cardNode.setUserData(recommendation.getId());
            return cardNode;
        } catch (IOException e) {
            throw new RuntimeException("Failed to render recommendation card", e);
        }
    }

    private void animateNewTopCard(Region cardNode) {
        dashboardQuickRecommendationsList.getChildren().add(0, cardNode);
        cardNode.applyCss();
        cardNode.layout();
        cardNode.setOpacity(0);
        cardNode.setTranslateY(-18);

        FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(240), cardNode);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(javafx.util.Duration.millis(240), cardNode);
        slide.setFromY(-18);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    private void animatePushDownTransition(Recommendation insertedRecommendation, List<Recommendation> finalRecommendations) {
        Region newCard = createRecommendationCard(insertedRecommendation, true);
        dashboardQuickRecommendationsList.getChildren().add(0, newCard);
        dashboardQuickRecommendationsList.applyCss();
        dashboardQuickRecommendationsList.layout();

        double cardHeight = newCard.getBoundsInParent().getHeight();
        double shiftAmount = cardHeight + dashboardQuickRecommendationsList.getSpacing();
        Region bottomCard = (Region) dashboardQuickRecommendationsList.getChildren()
                .get(dashboardQuickRecommendationsList.getChildren().size() - 1);

        FadeTransition fadeBottom = new FadeTransition(javafx.util.Duration.millis(320), bottomCard);
        fadeBottom.setFromValue(1);
        fadeBottom.setToValue(0);

        TranslateTransition pushDown = new TranslateTransition(javafx.util.Duration.millis(420), dashboardQuickRecommendationsList);
        pushDown.setFromY(-shiftAmount);
        pushDown.setToY(0);
        pushDown.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);

        ParallelTransition transition = new ParallelTransition(fadeBottom, pushDown);
        transition.setOnFinished(event -> {
            dashboardQuickRecommendationsList.setTranslateY(0);
            replaceRecommendationCards(finalRecommendations);
        });
        transition.play();
    }

    private List<Recommendation> newestFirst(List<Recommendation> recommendations) {
        List<Recommendation> ordered = new ArrayList<>(recommendations);
        Collections.reverse(ordered);
        return ordered;
    }

    @FXML
    private void handleWeeklyBudgetEdit() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Weekly Budget");
        dialog.setHeaderText("Set your weekly budget");
        dialog.getDialogPane().getButtonTypes().addAll(
                new ButtonType("Clear budget", ButtonBar.ButtonData.LEFT),
                ButtonType.CANCEL,
                ButtonType.OK
        );

        TextField amountField = new TextField(currentWeeklyBudgetGbp == null ? "" : String.format("%.2f", currentWeeklyBudgetGbp));
        amountField.setPromptText("Weekly budget in GBP");
        amountField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,6}(\\.\\d{0,2})?") ? change : null));

        Label helperText = new Label("Enter a weekly budget in GBP. Use Clear budget to remove it.");
        helperText.setWrapText(true);

        VBox content = new VBox(10, amountField, helperText);
        dialog.getDialogPane().setContent(content);

        dialog.showAndWait().ifPresent(result -> {
            try {
                if (result.getButtonData() == ButtonBar.ButtonData.LEFT) {
                    homeProfileRepository.setWeeklyBudgetGbp(HOME_ID, null);
                } else if (result == ButtonType.OK) {
                    String trimmed = amountField.getText().trim();
                    if (trimmed.isEmpty()) {
                        showValidationMessage("Please enter a number greater than 0, or use Clear budget.");
                        return;
                    }

                    double amount = Double.parseDouble(trimmed);
                    if (amount <= 0) {
                        showValidationMessage("Please enter a number greater than 0.");
                        return;
                    }
                    homeProfileRepository.setWeeklyBudgetGbp(HOME_ID, amount);
                } else {
                    return;
                }

                reloadWeeklyBudgetFromRepository();
                refreshWeeklyBudgetDisplay();
            } catch (NumberFormatException ex) {
                showValidationMessage("Please enter a valid number in GBP.");
            } catch (IllegalStateException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK);
                alert.setHeaderText("Unable to save weekly budget");
                alert.showAndWait();
            }
        });
    }

    private void reloadWeeklyBudgetFromRepository() {
        currentWeeklyBudgetGbp = homeProfileRepository.getWeeklyBudgetGbp(HOME_ID);
    }

    private void refreshWeeklyBudgetDisplay() {
        double spent = simulationService == null ? 0.0 : simulationService.getWeeklyAccumulatedCost();

        if (weeklyCostCardController != null) {
            weeklyCostCardController.setCost(spent);
        }

        if (dashboardWeeklyBudgetSpentLabel == null) {
            return;
        }

        dashboardWeeklyBudgetSpentLabel.setText(formatCurrency(spent) + " spent");
        dashboardWeeklyBudgetEditButton.setText(currentWeeklyBudgetGbp == null ? "Set Budget" : "Edit Budget");
        applyBudgetProgressStyle("budget-progress-neutral");
        applyBudgetStatusStyle("budget-status-neutral");

        if (currentWeeklyBudgetGbp == null) {
            dashboardWeeklyBudgetBudgetLabel.setText("Budget Not set");
            dashboardWeeklyBudgetRemainingLabel.setText("Remaining --");
            dashboardWeeklyBudgetStatusLabel.setText("Set a budget");
            dashboardWeeklyBudgetHelperLabel.setText("Budget resets at the start of each simulated week.");
            dashboardWeeklyBudgetProgressBar.setProgress(0);
            return;
        }

        dashboardWeeklyBudgetBudgetLabel.setText("Budget " + formatCurrency(currentWeeklyBudgetGbp));
        double remaining = currentWeeklyBudgetGbp - spent;
        if (remaining >= 0) {
            dashboardWeeklyBudgetRemainingLabel.setText("Remaining " + formatCurrency(remaining));
        } else {
            dashboardWeeklyBudgetRemainingLabel.setText("Over by " + formatCurrency(Math.abs(remaining)));
        }

        double usageRatio = spent / currentWeeklyBudgetGbp;
        dashboardWeeklyBudgetProgressBar.setProgress(Math.min(usageRatio, 1.0));
        dashboardWeeklyBudgetHelperLabel.setText("Budget resets at the start of each simulated week.");
        if (usageRatio < 0.8) {
            dashboardWeeklyBudgetStatusLabel.setText("Within budget");
            applyBudgetStatusStyle("budget-status-within");
            applyBudgetProgressStyle("budget-progress-within");
        } else if (usageRatio < 1.0) {
            dashboardWeeklyBudgetStatusLabel.setText("Close to limit");
            applyBudgetStatusStyle("budget-status-close");
            applyBudgetProgressStyle("budget-progress-close");
        } else {
            dashboardWeeklyBudgetStatusLabel.setText("Over budget");
            applyBudgetStatusStyle("budget-status-over");
            applyBudgetProgressStyle("budget-progress-over");
        }
    }

    private void applyBudgetStatusStyle(String styleClass) {
        dashboardWeeklyBudgetStatusLabel.getStyleClass().removeAll(
                "budget-status-within",
                "budget-status-close",
                "budget-status-over",
                "budget-status-neutral"
        );
        dashboardWeeklyBudgetStatusLabel.getStyleClass().add(styleClass);
    }

    private void applyBudgetProgressStyle(String styleClass) {
        dashboardWeeklyBudgetProgressBar.getStyleClass().removeAll(
                "budget-progress-within",
                "budget-progress-close",
                "budget-progress-over",
                "budget-progress-neutral"
        );
        dashboardWeeklyBudgetProgressBar.getStyleClass().add(styleClass);
    }

    private String formatCurrency(double amount) {
        return String.format("\u00A3%.2f", amount);
    }

    private void showValidationMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setHeaderText("Invalid budget value");
        alert.showAndWait();
    }
}
