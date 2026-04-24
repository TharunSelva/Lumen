package com.energyapp.controller;

import com.energyapp.db.ForecastIntervalRepository;
import com.energyapp.db.ForecastWindowRecord;
import com.energyapp.model.EnergyRecord;
import com.energyapp.model.Insight;
import com.energyapp.service.ForecastOverviewService;
import com.energyapp.service.InsightEngine;
import com.energyapp.service.SimulationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForecastingController {

    private static final int REFRESH_THRESHOLD = 5;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private LineChart<String, Number> energyCostForecastLineChart;
    @FXML private Label lastUpdatedLabel;
    @FXML private VBox insightsContainer;
    @FXML private Button forecastingReload;
    @FXML private LineChart<Number, Number> solarForecastChart;
    @FXML private Label solarForecastStatusLabel;
    @FXML private Label tomorrowSummaryHeadlineLabel;
    @FXML private VBox tomorrowSummaryList;

    private SimulationService simulationService;
    private int lastRefreshRecordCount = 0;

    private final ForecastIntervalRepository forecastIntervalRepository = new ForecastIntervalRepository();
    private final ForecastOverviewService forecastOverviewService = new ForecastOverviewService();

    @FXML
    public void initialize() {
        configureEnergyPriceChart();
        configureSolarChart();
        refreshForecastOverviewPanels();

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
        lastRefreshRecordCount = service.getEnergyStatsSessionHistory().size();

        service.getCurrentEnergyRecord().addListener((obs, oldRec, newRec) -> {
            if (newRec != null) {
                Platform.runLater(() -> {
                    refreshEnergyPriceChart();
                    refreshForecastOverviewPanels();
                    updateLastUpdatedLabel();

                    int currentSize = service.getEnergyStatsSessionHistory().size();
                    if (currentSize - lastRefreshRecordCount >= REFRESH_THRESHOLD) {
                        refreshInsights();
                        lastRefreshRecordCount = currentSize;
                    }
                });
            }
        });
    }

    private void configureEnergyPriceChart() {
        if (energyCostForecastLineChart == null) {
            return;
        }

        energyCostForecastLineChart.setAnimated(false);

        CategoryAxis axis = (CategoryAxis) energyCostForecastLineChart.getXAxis();
        axis.setCategories(FXCollections.observableArrayList(buildAllSlots()));

        NumberAxis yAxis = (NumberAxis) energyCostForecastLineChart.getYAxis();
        yAxis.setForceZeroInRange(false);
    }

    private void configureSolarChart() {
        if (solarForecastChart == null) {
            return;
        }

        solarForecastChart.setAnimated(false);
        solarForecastChart.setCreateSymbols(false);
        solarForecastChart.setLegendVisible(false);

        NumberAxis xAxis = (NumberAxis) solarForecastChart.getXAxis();
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(48);
        xAxis.setTickUnit(4);
    }

    private void refreshAllForecastPanels() {
        refreshForecastDataPanels();
    }

    private void refreshForecastDataPanels() {
        refreshInsights();
        refreshEnergyPriceChart();
        refreshForecastOverviewPanels();
        updateLastUpdatedLabel();
    }

    private void refreshInsights() {
        if (simulationService == null || insightsContainer == null) {
            return;
        }
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

        InsightEngine engine = new InsightEngine();
        List<Insight> activeInsights = engine.generateInsights(currentUsageTotal, previousUsageTotal);

        List<EnergyRecord> upcomingRecords = simulationService.getUpcomingRecords(48);
        Insight greenInsight = engine.generateGreenForecastInsight(upcomingRecords);
        if (greenInsight != null) {
            activeInsights.add(0, greenInsight);
        }

        boolean isFirstCard = true;
        for (Insight insight : activeInsights) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/energyapp/view/InsightCard.fxml"));
                HBox card = loader.load();

                InsightCardController cardController = loader.getController();

                Color statusColor = insight.getTitle().contains("Alert") ? Color.ORANGE : Color.LIGHTGREEN;
                cardController.setContent(insight.getDescription(), statusColor);

                insightsContainer.getChildren().add(card);

                if (isFirstCard && insight.getTitle().equals("Green Forecast")) {
                    Separator divider = new Separator();
                    divider.setPadding(new Insets(5, 0, 5, 0));
                    insightsContainer.getChildren().add(divider);
                    isFirstCard = false;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void refreshEnergyPriceChart() {
        if (simulationService == null || energyCostForecastLineChart == null) {
            return;
        }

        List<EnergyRecord> history = new ArrayList<>(simulationService.getEnergyStatsSessionHistory());
        EnergyRecord currentRecord = simulationService.getCurrentEnergyRecord().get();

        LocalDate chartDay = resolveChartDay(history, currentRecord);

        List<EnergyRecord> forecastRecords = loadForecastRecords(chartDay);
        List<XYChart.Series<String, Number>> chartSeries =
                buildPriceChartSeries(history, forecastRecords, chartDay);

        energyCostForecastLineChart.getData().setAll(chartSeries);

        if (chartSeries.isEmpty()) {
            energyCostForecastLineChart.getData().clear();
        }
    }

    private List<EnergyRecord> loadForecastRecords(LocalDate chartDay) {
        ForecastOverviewService.ForecastOverviewSnapshot snapshot =
                forecastOverviewService.load(ForecastOverviewService.HOME_ID, resolvePreferredForecastReference());

        List<EnergyRecord> forecastRecords = forecastIntervalRepository.fetchTariffForecast(
                ForecastOverviewService.HOME_ID,
                chartDay,
                snapshot.homeZone()
        );

        if (!forecastRecords.isEmpty()) {
            return forecastRecords;
        }

        return forecastIntervalRepository.fetchTodayTariffForecast();
    }

    private void refreshForecastOverviewPanels() {
        ForecastOverviewService.ForecastOverviewSnapshot snapshot =
                forecastOverviewService.load(ForecastOverviewService.HOME_ID, resolvePreferredForecastReference());

        renderSolarForecast(snapshot);
        renderTomorrowSummary(snapshot);
    }

    private LocalDateTime resolvePreferredForecastReference() {
        if (simulationService != null && simulationService.getCurrentEnergyRecord().get() != null) {
            return simulationService.getCurrentEnergyRecord().get().getTimestamp();
        }
        return null;
    }

    private void renderSolarForecast(ForecastOverviewService.ForecastOverviewSnapshot snapshot) {
        if (solarForecastChart == null) {
            return;
        }

        List<ForecastWindowRecord> solarWindows = snapshot.twoDayWindows().stream()
                .filter(window -> window.solarShortwaveWm2() != null)
                .toList();

        solarForecastChart.getData().clear();

        if (solarWindows.isEmpty()) {
            if (solarForecastStatusLabel != null) {
                solarForecastStatusLabel.setText("Solar forecast cache is still refreshing.");
            }
            return;
        }

        LocalDateTime dayStart = snapshot.anchorTime().toLocalDate().atStartOfDay();
        XYChart.Series<Number, Number> solarSeries = new XYChart.Series<>();

        for (ForecastWindowRecord window : solarWindows) {
            if (window.windowStart() == null) {
                continue;
            }
            double hourIndex = Duration.between(dayStart, window.windowStart()).toMinutes() / 60.0;
            if (hourIndex >= 0 && hourIndex <= 48) {
                solarSeries.getData().add(new XYChart.Data<>(hourIndex, window.solarShortwaveWm2()));
            }
        }

        solarForecastChart.getData().add(solarSeries);

        ForecastWindowRecord peakSolarTomorrow = snapshot.tomorrowWindows().stream()
                .filter(window -> window.solarShortwaveWm2() != null)
                .max(java.util.Comparator.comparingDouble(ForecastWindowRecord::solarShortwaveWm2))
                .orElse(null);

        if (solarForecastStatusLabel != null) {
            solarForecastStatusLabel.setText(peakSolarTomorrow == null
                    ? "Solar outlook loaded from the next 48 forecast hours."
                    : "Tomorrow peaks near " + formatTime(peakSolarTomorrow.windowStart())
                    + " at " + formatSolar(peakSolarTomorrow.solarShortwaveWm2()) + ".");
        }
    }

    private void renderTomorrowSummary(ForecastOverviewService.ForecastOverviewSnapshot snapshot) {
        if (tomorrowSummaryHeadlineLabel == null || tomorrowSummaryList == null) {
            return;
        }

        ForecastOverviewService.TomorrowSummarySnapshot tomorrowSnapshot =
                forecastOverviewService.buildTomorrowSummary(snapshot);
        tomorrowSummaryHeadlineLabel.setText(tomorrowSnapshot.headline());
        populateSummaryItems(tomorrowSummaryList, tomorrowSnapshot.items(), "forecast-summary-chip");
    }

    private void populateSummaryItems(VBox container, List<String> items, String styleClass) {
        container.getChildren().clear();
        for (String item : items) {
            Label label = new Label(item);
            label.setWrapText(true);
            label.getStyleClass().add(styleClass);
            container.getChildren().add(label);
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
        return timestamp == null ? "later" : timestamp.toLocalTime().format(TIME_FMT);
    }

    private String formatSlot(int slotIndex) {
        LocalTime time = LocalTime.MIDNIGHT.plusMinutes(slotIndex * 30L);
        return time.format(TIME_FMT);
    }

    private String formatSolar(Double solar) {
        return solar == null ? "unknown solar output" : String.format(java.util.Locale.UK, "%.0f W/m2", solar);
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
            EnergyRecord currentRecord = simulationService == null ? null : simulationService.getCurrentEnergyRecord().get();
            if (currentRecord != null && currentRecord.getTimestamp() != null) {
                lastUpdatedLabel.setText("Simulation updated " + formatTime(currentRecord.getTimestamp()));
            } else {
                lastUpdatedLabel.setText("Waiting for simulation data...");
            }
        }
    }
}
