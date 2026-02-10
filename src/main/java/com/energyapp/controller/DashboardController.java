package com.energyapp.controller;

import com.energyapp.service.SimulationService;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.chart.NumberAxis;

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
    private MainLayoutController mainController;
    private SimulationService simulationService;

    private final XYChart.Series<Number, Number> series = new XYChart.Series<>();

    private long windowSeconds = 60 * 60;  // last hour
    private long latestEpochSec = 0;
    private boolean chartWired = false;

    @FXML
    public void initialize() {
        System.out.println("DashboardController loaded successfully.");
        dashboardUsageLineChart.getData().add(series);
        setupTimeAxis(dashboardUsageXAxis);

        // click anywhere on chart opens statistics screen
        //dashboardUsageLineChart.setOnMouseClicked(e -> openEnergyStatistics());
        dashboardUsageLineChart.setOnMouseClicked(e -> {
            if (mainController != null) {
                mainController.handleShowStatistics();
            }
        });


    }

    public void setMainController(MainLayoutController mainController) {
        this.mainController = mainController;
    }


    public void setSimService (SimulationService simulationService) {
        this.simulationService = simulationService;
        initUsageMiniChart();

        if (weeklyCostCardController != null) {
            weeklyCostCardController.setCost(simulationService.getTotalAccumulatedCost());
        }

        simulationService.getCurrentEnergyRecord().addListener((observable, oldRecord, newRecord) -> {
            if (newRecord != null && weeklyCostCardController != null) {
                double newTotal = simulationService.getTotalAccumulatedCost();
                weeklyCostCardController.setCost(newTotal);
            }
        });
    }

    private void initUsageMiniChart() {
        // TODO:  will fill this in
    }

}

