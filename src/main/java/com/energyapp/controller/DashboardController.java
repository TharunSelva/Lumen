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

    private double weeklyCostAccumulator = 0.0;

    @FXML
    public void initialize() {
        System.out.println("DashboardController loaded successfully.");
    }

    public void setSimService (SimulationService simulationService) {
        simulationService.getCurrentEnergyRecord().addListener((observable, oldRecord, newRecord) -> {
            double importKwh = newRecord.getGridImport();
            double price = newRecord.getPricePerKwh();
            double cost = (importKwh * price) / 100;

            weeklyCostAccumulator = weeklyCostAccumulator + cost;
            weeklyCostCardController.setCost(weeklyCostAccumulator);
        });
    }
}

