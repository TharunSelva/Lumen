package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import javafx.scene.shape.Circle;

public class DashboardController {

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
    @FXML private Label dashboardQuickRec1Label;
    @FXML private Label dashboardQuickRec2Label;
    @FXML private Label dashboardQuickRec3Label;
    @FXML private VBox dashboardQuickRecContainerVBox;
    @FXML private GridPane dashboardQuickRecGridPane;
    @FXML private Label dashboardQuickRecTitleLabel;
    @FXML private VBox dashboardSidebarVBox;
    @FXML private Label dashboardUsageInsightRightLabel;
    @FXML private VBox dashboardUsageInsightRightVBox;
    @FXML private GridPane dashboardUsageInsightsGridPane;

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

        // click anywhere on chart opens statistics screen
        dashboardUsageLineChart.setOnMouseClicked(e -> openEnergyStatistics());
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
            double currentCost = (simulationService.getCurrentEnergyRecord().get().getGridImport() * simulationService.getCurrentEnergyRecord().get().getPricePerKwh()) / 100;
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
        axis.setForceZeroInRange(false);
        axis.setTickUnit(10 * 60);

        axis.setTickLabelFormatter(new StringConverter<>() {
            @Override public String toString(Number object) {
                long sec = object.longValue();
                LocalTime t = Instant.ofEpochSecond(sec)
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime();
                return t.toString().substring(0, 5);
            }
            @Override public Number fromString(String string) { return 0; }
        });
    }

    private long toEpochSec(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private void plotDataPoint(EnergyRecord rec) {
        long x = toEpochSec(rec.getTimestamp());
        double y = rec.getWatts();
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


}

