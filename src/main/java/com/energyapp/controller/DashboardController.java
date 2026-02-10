package com.energyapp.controller;

import com.energyapp.service.SimulationService;
import com.energyapp.service.PriceUpdateService;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    @FXML private LineChart<?, ?> dashboardUsageLineChart;
    @FXML private Label dashboardUsageTitleLabel;
    @FXML private CostCardController weeklyCostCardController;
    @FXML private Label priceStatusText;
    @FXML private Circle priceIndicator;

    private PriceIndicatorController indicatorLogic;
    private PriceUpdateService priceService;

    @FXML
    public void initialize() {
        indicatorLogic = new PriceIndicatorController(priceStatusText, priceIndicator);
        priceService = new PriceUpdateService();
        priceService.setOnSucceeded(event -> {
            Double newPrice = priceService.getValue();
            indicatorLogic.updateStatus(newPrice);
        });
        priceService.start();
        System.out.println("DashboardController loaded successfully.");
    }

    public void setSimService (SimulationService simulationService) {
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
}

