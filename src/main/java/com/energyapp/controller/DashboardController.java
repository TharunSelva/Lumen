package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import com.energyapp.model.Recommendation;
import com.energyapp.service.RecommendationFeedService;
import com.energyapp.service.SimulationService;
import com.energyapp.utils.EnergyCalculator;
import com.energyapp.utils.DateTimeUtils;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.scene.shape.Circle;

public class DashboardController {

    private static final int HOME_VISIBLE_RECOMMENDATIONS = 3;

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

    //@FXML private LineChart<?, ?> dashboardUsageLineChart;
    @FXML private LineChart<Number, Number> dashboardUsageLineChart;
    @FXML private NumberAxis dashboardUsageXAxis;
    @FXML private NumberAxis dashboardUsageYAxis;

    @FXML private Label dashboardUsageTitleLabel;
    @FXML private CostCardController weeklyCostCardController;
    @FXML private Label priceStatusText;
    @FXML private Circle priceIndicator;

    private PriceIndicatorController indicatorLogic;
    private MainLayoutController mainController;
    private SimulationService simulationService;
    private final RecommendationFeedService recommendationFeedService = RecommendationFeedService.getInstance();

    private final XYChart.Series<Number, Number> series = new XYChart.Series<>();

    private long windowSeconds = 60 * 60;  // last hour
    private long latestEpochSec = 0;
    private boolean chartWired = false;

    @FXML
    public void initialize() {
        indicatorLogic = new PriceIndicatorController(priceStatusText, priceIndicator);
        System.out.println("DashboardController loaded successfully.");
        dashboardUsageLineChart.getData().add(series);
        setupTimeAxis(dashboardUsageXAxis);
        dashboardUsageXAxis.setLabel("Time");
        dashboardUsageYAxis.setLabel("Power (kWh)");
        dashboardUsageYAxis.setAutoRanging(true);

        // click anywhere on chart opens statistics screen
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

        dashboardScrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            dashboardContentVBox.setPrefWidth(newBounds.getWidth());
        });

        if (dashboardQuickRecommendationsViewport != null) {
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(dashboardQuickRecommendationsViewport.widthProperty());
            clip.heightProperty().bind(dashboardQuickRecommendationsViewport.heightProperty());
            dashboardQuickRecommendationsViewport.setClip(clip);
        }
    }

    private void openEnergyStatistics() {
        if (mainController != null) {
            mainController.handleShowStatistics();
        }
    }


    public void setMainController(MainLayoutController mainController) {
        this.mainController = mainController;
    }


    public void setSimService (SimulationService simulationService) {
        this.simulationService = simulationService;
        initUsageMiniChart();

        if (simulationService.getCurrentEnergyRecord().get() != null) {
            double currentCost = EnergyCalculator.calculateCostInPounds(simulationService.getCurrentEnergyRecord().get().getGridImport(), simulationService.getCurrentEnergyRecord().get().getPricePerKwh());
            indicatorLogic.updateIndicatorByCost(currentCost);

            if (weeklyCostCardController != null) {
                weeklyCostCardController.setCost(simulationService.getTotalAccumulatedCost());
            }
        }

        // 2. CONTINUE LISTENING: Keep the listeners for future updates
        simulationService.currentIntervalCostProperty().addListener((obs, oldCost, newCost) -> {
            if (newCost != null) {
                indicatorLogic.updateIndicatorByCost(newCost.doubleValue());
            }
        });

        if (weeklyCostCardController != null) {
            weeklyCostCardController.setCost(simulationService.getTotalAccumulatedCost());
        }

        simulationService.currentIntervalCostProperty().addListener((obs, oldCost, newCost) -> {
            if (newCost != null) {
                indicatorLogic.updateIndicatorByCost(newCost.doubleValue());
            }
        });

        simulationService.getCurrentEnergyRecord().addListener((observable, oldRecord, newRecord) -> {
            if (newRecord != null && weeklyCostCardController != null) {
                double newTotal = simulationService.getTotalAccumulatedCost();
                weeklyCostCardController.setCost(newTotal);
            }
        });

        updateDashboardRecommendations(simulationService.getCurrentEnergyRecord().get());
        simulationService.getCurrentEnergyRecord().addListener((obs, oldRecord, newRecord) -> updateDashboardRecommendations(newRecord));
    }


    // **MINI CHART WIRING METHODS:**
    private void initUsageMiniChart() {
        if (simulationService == null || chartWired) return;
        chartWired = true;

        // initial history
        List<EnergyRecord> history = simulationService.getEnergyStatsSessionHistory();
        for (EnergyRecord rec : history) {
            plotDataPoint(rec);
        }

        // live updates
        simulationService.getCurrentEnergyRecord().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                javafx.application.Platform.runLater(() -> plotDataPoint(newVal));
            }
        });
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
        if (latestEpochSec == 0) return;

        long ub = latestEpochSec;
        long lb = ub - windowSeconds;

        dashboardUsageXAxis.setLowerBound(lb);
        dashboardUsageXAxis.setUpperBound(ub);

        trimToWindow(lb);
    }

    private void trimToWindow(long lb) {
        while (!series.getData().isEmpty()) {
            long t = series.getData().get(0).getXValue().longValue();
            if (t >= lb) break;
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
        Region bottomCard = (Region) dashboardQuickRecommendationsList.getChildren().get(dashboardQuickRecommendationsList.getChildren().size() - 1);

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


}
