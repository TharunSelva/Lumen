package com.energyapp.controller;

import com.energyapp.db.ForecastIntervalRepository;
import com.energyapp.db.HomeProfileRepository;
import com.energyapp.model.EnergyRecord;
import com.energyapp.model.Geocode;
import com.energyapp.model.Insight;
import com.energyapp.service.InsightEngine;
import com.energyapp.db.OpenMeteoSolarService;
import com.energyapp.service.SimulationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.collections.FXCollections;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForecastingController {

    @FXML private LineChart<String, Number> energyCostForecastLineChart;
    @FXML private Label lastUpdatedLabel;
    @FXML private VBox insightsContainer;
    @FXML private Button forecastingReload;
    @FXML private XYChart<Number, Number> solarForecastChart;

    private SimulationService simulationService;
    private final ForecastIntervalRepository forecastIntervalRepository = new ForecastIntervalRepository();
    private static final String HOME_ID = "HOME_1";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final HomeProfileRepository homeRepo = new HomeProfileRepository();
    private final OpenMeteoSolarService solarService = new OpenMeteoSolarService();

    @FXML
    public void initialize() {
        if (energyCostForecastLineChart != null) {
            energyCostForecastLineChart.setAnimated(false);
            CategoryAxis axis = (CategoryAxis) energyCostForecastLineChart.getXAxis();
            axis.setCategories(FXCollections.observableArrayList(buildAllSlots()));
        }

        loadSolarForecast();

        if (lastUpdatedLabel != null) {
            lastUpdatedLabel.setText("Waiting for simulation data...");
        }

        if (forecastingReload != null) {
            forecastingReload.setOnAction(event -> refreshAllForecastPanels());
        }
    }

    public void setSimulationService(SimulationService service) {
        this.simulationService = service;
        refreshForecastDataPanels();

        service.getCurrentEnergyRecord().addListener((obs, oldRec, newRec) -> {
            if (newRec != null) {
                refreshForecastDataPanels();
            }
        });
    }

    private void refreshAllForecastPanels() {
        loadSolarForecast();
        refreshForecastDataPanels();
    }

    private void refreshForecastDataPanels() {
        refreshInsights();
        refreshEnergyPriceChart();
        updateLastUpdatedLabel();
    }

    private void refreshInsights() {
        if (simulationService == null) return;
        if (insightsContainer.getChildren().size() > 1) {
            insightsContainer.getChildren().remove(1, insightsContainer.getChildren().size());
        }

        List<EnergyRecord> history = simulationService.getEnergyStatsSessionHistory();

        double currentUsageTotal = 0.0;
        double previousUsageTotal = 0.0;

        if (history.size() >= 10) {
            int mid = history.size() - 5;
            for (int i = history.size() - 1; i >= mid; i--) {
                currentUsageTotal += history.get(i).getEvChargerUsageKwh()
                        + history.get(i).getWashingMachineUsageKwh()
                        + history.get(i).getHeatingUsageKwh()
                        + history.get(i).getBaseloadKwh();
            }
            for (int i = mid - 1; i >= mid - 5; i--) {
                previousUsageTotal += history.get(i).getEvChargerUsageKwh()
                        + history.get(i).getWashingMachineUsageKwh()
                        + history.get(i).getHeatingUsageKwh()
                        + history.get(i).getBaseloadKwh();
            }
        }
        System.out.println("Previous total: " + previousUsageTotal + " kWh, Current total: " + currentUsageTotal + " kWh");
        InsightEngine engine = new InsightEngine();
        List<Insight> activeInsights = engine.generateInsights(currentUsageTotal, previousUsageTotal);

        for (Insight insight : activeInsights) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/energyapp/view/InsightCard.fxml"));
                HBox card = loader.load();

                InsightCardController cardController = loader.getController();

                Color statusColor = insight.getTitle().contains("Alert") ? Color.ORANGE : Color.LIGHTGREEN;
                cardController.setContent(insight.getDescription(), statusColor);

                insightsContainer.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void refreshEnergyPriceChart() {
        if (simulationService == null || energyCostForecastLineChart == null) return;

        List<EnergyRecord> history = new ArrayList<>(simulationService.getEnergyStatsSessionHistory());
        EnergyRecord currentRecord = simulationService.getCurrentEnergyRecord().get();

        LocalDate chartDay = resolveChartDay(history, currentRecord);
        List<EnergyRecord> forecastRecords = forecastIntervalRepository.fetchTodayTariffForecast();
        List<XYChart.Series<String, Number>> chartSeries = buildPriceChartSeries(history, forecastRecords, chartDay);

        energyCostForecastLineChart.getData().setAll(chartSeries);

        if (chartSeries.isEmpty()) {
            energyCostForecastLineChart.getData().clear();
        }
    }

    private List<XYChart.Series<String, Number>> buildContiguousSeries(Map<String, Number> valuesBySlot, String seriesName) {
        List<XYChart.Series<String, Number>> seriesList = new ArrayList<>();
        XYChart.Series<String, Number> currentSeries = null;

        for (int slot = 0; slot < 48; slot++) {
            String slotLabel = formatSlot(slot);
            Number value = valuesBySlot.get(slotLabel);

            if (value == null) {
                currentSeries = null;
                continue;
            }

            if (currentSeries == null) {
                currentSeries = new XYChart.Series<>();
                currentSeries.setName(seriesName);
                seriesList.add(currentSeries);
            }

            currentSeries.getData().add(new XYChart.Data<>(slotLabel, value));
        }

        return seriesList;
    }

    private LocalDate resolveChartDay(List<EnergyRecord> history, EnergyRecord currentRecord) {
        if (currentRecord != null && currentRecord.getTimestamp() != null) {
            return currentRecord.getTimestamp().toLocalDate();
        }

        for (int i = history.size() - 1; i >= 0; i--) {
            EnergyRecord record = history.get(i);
            if (record.getTimestamp() != null) {
                return record.getTimestamp().toLocalDate();
            }
        }

        return LocalDate.now();
    }

    List<XYChart.Series<String, Number>> buildPriceChartSeries(
            List<EnergyRecord> history,
            List<EnergyRecord> forecastRecords,
            LocalDate chartDay
    ) {
        Map<String, Number> actualPricesBySlot = collectPricesBySlot(history, chartDay);
        Map<String, Number> forecastPricesBySlot = collectForecastPricesBySlot(actualPricesBySlot, forecastRecords);

        List<XYChart.Series<String, Number>> chartSeries = new ArrayList<>();
        chartSeries.addAll(buildContiguousSeries(actualPricesBySlot, "Actual Price"));
        chartSeries.addAll(buildContiguousSeries(forecastPricesBySlot, "Forecast Price"));
        return chartSeries;
    }

    private Map<String, Number> collectPricesBySlot(List<EnergyRecord> history, LocalDate chartDay) {
        Map<String, Number> pricesBySlot = new HashMap<>();

        for (EnergyRecord record : history) {
            if (record.getTimestamp() != null && record.getTimestamp().toLocalDate().equals(chartDay)) {
                pricesBySlot.put(formatTime(record.getTimestamp()), record.getPricePerKwh());
            }
        }

        return pricesBySlot;
    }

    private Map<String, Number> collectForecastPricesBySlot(
            Map<String, Number> historicalPricesBySlot,
            List<EnergyRecord> forecastRecords
    ) {
        Map<String, Number> pricesBySlot = new HashMap<>();

        for (EnergyRecord record : forecastRecords) {
            LocalDateTime timestamp = record.getTimestamp();
            if (timestamp != null) {
                String slotLabel = formatTime(timestamp);
                if (!historicalPricesBySlot.containsKey(slotLabel)) {
                    pricesBySlot.put(slotLabel, record.getPricePerKwh());
                }
            }
        }

        return pricesBySlot;
    }

    private String formatTime(LocalDateTime timestamp) {
        return timestamp.toLocalTime().format(TIME_FMT);
    }

    private String formatSlot(int slotIndex) {
        LocalTime time = LocalTime.MIDNIGHT.plusMinutes(slotIndex * 30L);
        return time.format(TIME_FMT);
    }

    private List<String> buildAllSlots() {
        List<String> slots = new ArrayList<>();
        for (int slot = 0; slot < 48; slot++) {
            slots.add(formatSlot(slot));
        }
        return slots;
    }

    private void updateLastUpdatedLabel() {
        if (lastUpdatedLabel != null) {
            lastUpdatedLabel.setText("Updated " + LocalTime.now().format(TIME_FMT));
        }
    }

    private void loadSolarForecast() {
        Geocode loc = homeRepo.getHomeLocation("HOME_1");
        if (loc == null) return;

        List<OpenMeteoSolarService.SolarPoint> forecast = null;
        try {
            forecast = solarService.fetchHourlyShortwave(loc.latitude(), loc.longitude(), 2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (forecast == null) return;

        List<OpenMeteoSolarService.SolarPoint> finalForecast = forecast;
        Platform.runLater(() -> {
            solarForecastChart.getData().clear();

            // Lock the X axis to exactly your data range
            javafx.scene.chart.NumberAxis xAxis =
                    (javafx.scene.chart.NumberAxis) solarForecastChart.getXAxis();
            xAxis.setAutoRanging(false);
            xAxis.setLowerBound(0);
            xAxis.setUpperBound(47);
            xAxis.setTickUnit(1); // one tick per hour

            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            series.setName("Solar Generation (W/m²)");

            for (int i = 0; i < finalForecast.size(); i++) {
                OpenMeteoSolarService.SolarPoint point = finalForecast.get(i);
                int hour = Integer.parseInt(point.timeIso.substring(11, 13));
                boolean isDay2 = i >= 24;
                series.getData().add(new XYChart.Data<>(isDay2 ? hour + 24 : hour, point.shortwaveWm2));
            }

            solarForecastChart.getData().add(series);
        });
    }
}
