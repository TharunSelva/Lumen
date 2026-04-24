package com.energyapp.controller;

import com.energyapp.db.CanonicalDeviceMapper;
import com.energyapp.db.DeviceRepository;
import com.energyapp.db.ForecastWindowRecord;
import com.energyapp.db.HomeEnergyAssetsRepository;
import com.energyapp.db.HomeProfileRepository;
import com.energyapp.model.Device;
import com.energyapp.model.EnergyRecord;
import com.energyapp.model.Recommendation;
import com.energyapp.model.ScheduledDevice;
import com.energyapp.service.ForecastOverviewService;
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
import javafx.scene.Group;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DashboardController {

    private static final int HOME_VISIBLE_RECOMMENDATIONS = 3;
    private static final int HOME_FORECAST_WINDOWS = 6;
    private static final String HOME_ID = "HOME_1";
    private static final DateTimeFormatter DEVICE_TIME_FMT = DateTimeFormatter.ofPattern("EEE HH:mm");

    @FXML private VBox dashboard00VBox;
    @FXML private VBox dashboard10VBox;
    @FXML private GridPane dashboardMainGridPane;
    @FXML private Label dashboardGreetingLabel;
    @FXML private Label dashboardHeroSubtitleLabel;
    @FXML private ComboBox<String> zoomComboBox;
    @FXML private VBox dashboardContentVBox;
    @FXML private ScrollPane dashboardScrollPane;
    @FXML private LineChart<Number, Number> dashboardUsageLineChart;
    @FXML private NumberAxis dashboardUsageXAxis;
    @FXML private NumberAxis dashboardUsageYAxis;
    @FXML private Label dashboardUsageTitleLabel;
    @FXML private Label dashboardUsageInsightRightLabel;
    @FXML private Label dashboardUsageTotalEnergyLabel;
    @FXML private Label dashboardUsageSolarTodayLabel;
    @FXML private Label dashboardUsageTotalCostLabel;
    @FXML private Label dashboardUsageProjectedCostLabel;
    @FXML private Label dashboardUsageSourceSummaryLabel;
    @FXML private Label dashboardUsageHelperLabel;
    @FXML private VBox dashboardQuickRecommendationsList;
    @FXML private StackPane dashboardQuickRecommendationsViewport;
    @FXML private Label dashboardQuickRecTitleLabel;
    @FXML private Label dashboardEcoImpactTitleLabel;
    @FXML private VBox dashboardEcoImpactVBox;
    @FXML private Label dashboardEcoStatusLabel;
    @FXML private Label dashboardEcoIntensityLabel;
    @FXML private Label dashboardEcoRecommendationLabel;
    @FXML private Label dashboardEcoSummaryLabel;
    @FXML private Label dashboardQuickForecastTitleLabel;
    @FXML private VBox dashboardQuickForecastVBox;
    @FXML private Label dashboardForecastHeadlineLabel;
    @FXML private Label dashboardForecastStatusLabel;
    @FXML private HBox dashboardQuickForecastBarHBox;
    @FXML private VBox dashboardForecastHighlightsVBox;
    @FXML private VBox dashboardDevicesSummaryVBox;
    @FXML private Label dashboardDevicesTitleLabel;
    @FXML private Label dashboardDevicesOnCountLabel;
    @FXML private Label dashboardDevicesScheduledCountLabel;
    @FXML private Label dashboardDevicesTotalCountLabel;
    @FXML private Label dashboardDevicesNextRunLabel;
    @FXML private VBox dashboardDevicesHighlightsVBox;
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

    private Group zoomWrapper;
    private PriceIndicatorController indicatorLogic;
    private MainLayoutController mainController;
    private SimulationService simulationService;
    private final RecommendationFeedService recommendationFeedService = RecommendationFeedService.getInstance();
    private final HomeProfileRepository homeProfileRepository = new HomeProfileRepository();
    private final HomeEnergyAssetsRepository homeEnergyAssetsRepository = new HomeEnergyAssetsRepository();
    private final ForecastOverviewService forecastOverviewService = new ForecastOverviewService();
    private final DeviceRepository deviceRepository = new DeviceRepository();
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
        refreshDashboardPanels(newRecord);
        if (newRecord != null) {
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
        refreshGreeting();
        if (dashboardHeroSubtitleLabel != null) {
            dashboardHeroSubtitleLabel.setText("Live usage, eco signals, forecasts, and device activity in one place.");
        }

        dashboardUsageLineChart.getData().add(series);
        setupTimeAxis(dashboardUsageXAxis);
        dashboardUsageXAxis.setLabel("Time");
        dashboardUsageYAxis.setLabel("Power (kWh)");
        dashboardUsageYAxis.setAutoRanging(true);
        dashboardUsageLineChart.setOnMouseClicked(event -> openEnergyStatistics());

        zoomWrapper = new Group(dashboardContentVBox);
        dashboardScrollPane.setContent(zoomWrapper);
        configureZoomControls();
        configureDashboardClipping();

        reloadWeeklyBudgetFromRepository();
        refreshWeeklyBudgetDisplay();
        refreshDashboardPanels(null);
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

        EnergyRecord currentRecord = simulationService == null ? null : simulationService.getCurrentEnergyRecord().get();
        if (currentRecord != null) {
            double currentCost = EnergyCalculator.calculateCostInPounds(
                    currentRecord.getGridImport(),
                    currentRecord.getPricePerKwh()
            );
            indicatorLogic.updateIndicatorByCost(currentCost);
        }

        if (simulationService != null) {
            simulationService.currentIntervalCostProperty().addListener(intervalCostListener);
            simulationService.weeklyAccumulatedCostProperty().addListener(weeklyCostListener);
            simulationService.getCurrentEnergyRecord().addListener(dashboardRecordListener);
        }

        refreshWeeklyBudgetDisplay();
        updateDashboardRecommendations(currentRecord);
        refreshDashboardPanels(currentRecord);
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

    static String buildGreeting(String firstName, LocalTime currentTime) {
        String cleanedName = firstName == null ? "" : firstName.trim();
        String dayGreeting = currentTime.isBefore(LocalTime.NOON)
                ? "Good morning"
                : currentTime.isBefore(LocalTime.of(18, 0))
                ? "Good afternoon"
                : "Good evening";

        return cleanedName.isEmpty()
                ? dayGreeting + "!"
                : dayGreeting + ", " + cleanedName + "!";
    }

    static double calculateProjectedDailyCost(double totalCostToday, int todayRecordCount) {
        double elapsedHours = todayRecordCount * 0.5;
        return elapsedHours <= 0.0 ? 0.0 : (totalCostToday / elapsedHours) * 24.0;
    }

    static DashboardDeviceSnapshot buildDeviceSnapshot(List<Device> devices, LocalDateTime referenceTime) {
        List<Device> safeDevices = devices == null ? List.of() : devices;
        LocalDateTime effectiveReference = referenceTime == null ? LocalDateTime.now() : referenceTime;

        long onCount = safeDevices.stream().filter(Device::isOn).count();
        long scheduledCount = safeDevices.stream()
                .filter(device -> "SCHEDULED".equals(presentationStatus(device)))
                .count();

        String nextRun = safeDevices.stream()
                .filter(ScheduledDevice.class::isInstance)
                .map(ScheduledDevice.class::cast)
                .filter(device -> device.getSchedule() != null && !device.isSkipNextRun())
                .map(device -> nextRunTime(device, effectiveReference))
                .min(LocalDateTime::compareTo)
                .map(time -> "Next run " + time.format(DEVICE_TIME_FMT))
                .orElse("No scheduled run yet");

        List<String> onNowNames = safeDevices.stream()
                .filter(Device::isOn)
                .map(DashboardController::displayNameForDevice)
                .limit(3)
                .toList();

        List<String> scheduledNames = safeDevices.stream()
                .filter(ScheduledDevice.class::isInstance)
                .map(ScheduledDevice.class::cast)
                .filter(device -> device.getSchedule() != null && !device.isSkipNextRun())
                .map(device -> displayNameForDevice(device) + " at " + device.getSchedule())
                .limit(3)
                .toList();

        List<String> highlights = new ArrayList<>();
        if (!onNowNames.isEmpty()) {
            highlights.add("On now: " + String.join(", ", onNowNames));
        } else {
            highlights.add("Nothing is actively running right now.");
        }
        if (!scheduledNames.isEmpty()) {
            highlights.add("Scheduled next: " + String.join(" • ", scheduledNames));
        } else if (!safeDevices.isEmpty()) {
            highlights.add("No saved schedules yet for your current devices.");
        }
        if (safeDevices.isEmpty()) {
            highlights.add("Add devices to track what is on, scheduled, and ready to shift.");
        }

        return new DashboardDeviceSnapshot(
                (int) onCount,
                (int) scheduledCount,
                safeDevices.size(),
                nextRun,
                List.copyOf(highlights)
        );
    }

    record DashboardDeviceSnapshot(
            int onCount,
            int scheduledCount,
            int totalCount,
            String nextRunLabel,
            List<String> highlights
    ) {
    }

    private void openEnergyStatistics() {
        if (mainController != null) {
            mainController.handleShowStatistics();
        }
    }

    private void configureZoomControls() {
        zoomComboBox.getItems().addAll("100%", "125%", "150%", "175%");
        zoomComboBox.setValue("100%");
        zoomComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                double newZoomLevel = Double.parseDouble(newVal.replace("%", "")) / 100;
                dashboardContentVBox.setScaleX(newZoomLevel);
                dashboardContentVBox.setScaleY(newZoomLevel);
                dashboardScrollPane.requestLayout();
            }
        });

        dashboardScrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            dashboardContentVBox.setPrefWidth(newBounds.getWidth());
            dashboardScrollPane.requestLayout();
        });
    }

    private void configureDashboardClipping() {
        if (dashboardQuickRecommendationsViewport != null) {
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(dashboardQuickRecommendationsViewport.widthProperty());
            clip.heightProperty().bind(dashboardQuickRecommendationsViewport.heightProperty());
            dashboardQuickRecommendationsViewport.setClip(clip);
        }
    }

    private void initUsageMiniChart() {
        if (simulationService == null || chartWired) {
            return;
        }
        chartWired = true;
        series.getData().clear();
        latestEpochSec = 0;

        for (EnergyRecord record : getAvailableHistory()) {
            plotDataPoint(record);
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

    private void plotDataPoint(EnergyRecord record) {
        long x = DateTimeUtils.toEpochSeconds(record.getTimestamp());
        double y = record.getTotalLoadKwh();
        latestEpochSec = x;

        series.getData().add(new XYChart.Data<>(x, y));
        updateAxisBounds();
    }

    private void updateAxisBounds() {
        if (latestEpochSec == 0) {
            return;
        }

        long upperBound = latestEpochSec;
        long lowerBound = upperBound - windowSeconds;

        dashboardUsageXAxis.setLowerBound(lowerBound);
        dashboardUsageXAxis.setUpperBound(upperBound);
        trimToWindow(lowerBound);
    }

    private void trimToWindow(long lowerBound) {
        while (!series.getData().isEmpty()) {
            long timestamp = series.getData().get(0).getXValue().longValue();
            if (timestamp >= lowerBound) {
                break;
            }
            series.getData().remove(0);
        }
    }

    private void refreshDashboardPanels(EnergyRecord currentRecord) {
        refreshGreeting();
        refreshUsageSummary();
        refreshEcoSummary(currentRecord);
        refreshForecastSummary(currentRecord);
        refreshDeviceSummary(currentRecord);
    }

    private void refreshUsageSummary() {
        if (dashboardUsageTotalEnergyLabel == null
                && dashboardUsageSolarTodayLabel == null
                && dashboardUsageTotalCostLabel == null
                && dashboardUsageProjectedCostLabel == null
                && dashboardUsageInsightRightLabel == null
                && dashboardUsageSourceSummaryLabel == null
                && dashboardUsageHelperLabel == null) {
            return;
        }

        DashboardUsageSnapshot usageSnapshot = buildUsageSnapshot();
        EnergyStatsController.SourceBreakdown sourceBreakdown = usageSnapshot.summarySnapshot().sourceBreakdown();

        if (dashboardUsageTotalEnergyLabel != null) {
            dashboardUsageTotalEnergyLabel.setText(String.format(Locale.UK, "%.1f kWh", usageSnapshot.summarySnapshot().totalEnergyToday()));
        }
        if (dashboardUsageSolarTodayLabel != null) {
            dashboardUsageSolarTodayLabel.setText(String.format(Locale.UK, "%.1f kWh", usageSnapshot.summarySnapshot().totalSolarToday()));
        }
        if (dashboardUsageTotalCostLabel != null) {
            dashboardUsageTotalCostLabel.setText(formatCurrency(usageSnapshot.summarySnapshot().totalCostToday()));
        }
        if (dashboardUsageProjectedCostLabel != null) {
            dashboardUsageProjectedCostLabel.setText(formatCurrency(usageSnapshot.projectedDailyCost()));
        }
        if (dashboardUsageInsightRightLabel != null) {
            dashboardUsageInsightRightLabel.setText(buildUsageInsightLine(usageSnapshot));
        }
        if (dashboardUsageSourceSummaryLabel != null) {
            dashboardUsageSourceSummaryLabel.setText(String.format(
                    Locale.UK,
                    "Today's source mix: %d%% solar • %d%% battery • %d%% grid",
                    sourceBreakdown.solarPercentDisplay(),
                    sourceBreakdown.batteryPercentDisplay(),
                    sourceBreakdown.gridPercentDisplay()
            ));
        }
        if (dashboardUsageHelperLabel != null) {
            dashboardUsageHelperLabel.setText(usageSnapshot.todayRecordCount() == 0
                    ? "Usage totals will appear once live simulation data starts flowing."
                    : "Click the graph to open the full statistics page.");
        }
    }

    private DashboardUsageSnapshot buildUsageSnapshot() {
        List<EnergyRecord> history = getAvailableHistory();
        EnergyStatsController.SummarySnapshot summarySnapshot = EnergyStatsController.buildSummarySnapshot(
                history,
                homeEnergyAssetsRepository.fetchByHomeId(HOME_ID)
        );
        int todayRecordCount = EnergyStatsController.getLatestDayRecordsForHistory(history).size();
        return new DashboardUsageSnapshot(
                summarySnapshot,
                calculateProjectedDailyCost(summarySnapshot.totalCostToday(), todayRecordCount),
                todayRecordCount
        );
    }

    private void refreshEcoSummary(EnergyRecord currentRecord) {
        if (dashboardEcoStatusLabel == null
                || dashboardEcoIntensityLabel == null
                || dashboardEcoRecommendationLabel == null
                || dashboardEcoSummaryLabel == null) {
            return;
        }

        DashboardUsageSnapshot usageSnapshot = buildUsageSnapshot();
        EnergyStatsController.SourceBreakdown sourceBreakdown = usageSnapshot.summarySnapshot().sourceBreakdown();

        if (currentRecord == null) {
            dashboardEcoStatusLabel.getStyleClass().setAll("eco-hour-pill", "eco-hour-moderate");
            dashboardEcoStatusLabel.setText("Awaiting live eco signal");
            dashboardEcoIntensityLabel.setText("Grid intensity will appear when live data arrives.");
            dashboardEcoRecommendationLabel.setText("Eco recommendations adapt to the current grid intensity and solar share.");
            dashboardEcoSummaryLabel.setText("Today's source mix will appear as usage data builds.");
            return;
        }

        EcoStatisticsController.HourStatus hourStatus = EcoStatisticsController.determineHourStatus(currentRecord.getCarbon());
        dashboardEcoStatusLabel.getStyleClass().setAll("eco-hour-pill", hourStatus.styleClass());
        dashboardEcoStatusLabel.setText(hourStatus.status());
        dashboardEcoIntensityLabel.setText(String.format(Locale.UK, "%.0f gCO2/kWh right now", currentRecord.getCarbon()));

        List<Recommendation> ecoRecommendations = EcoStatisticsController.buildEcoRecommendations(currentRecord);
        dashboardEcoRecommendationLabel.setText(
                ecoRecommendations.isEmpty() ? hourStatus.recommendation() : ecoRecommendations.get(0).getExplanation()
        );

        double cleanShareNow = EnergyCalculator.calculateCleanShare(
                currentRecord.getTotalLoadKwh(),
                currentRecord.getSolarGenKwh()
        );
        dashboardEcoSummaryLabel.setText(String.format(
                Locale.UK,
                "Clean share now: %.0f%% • Today so far: %d%% solar, %d%% battery, %d%% grid.",
                cleanShareNow,
                sourceBreakdown.solarPercentDisplay(),
                sourceBreakdown.batteryPercentDisplay(),
                sourceBreakdown.gridPercentDisplay()
        ));
    }

    private void refreshForecastSummary(EnergyRecord currentRecord) {
        if (dashboardForecastHeadlineLabel == null
                || dashboardForecastStatusLabel == null
                || dashboardQuickForecastBarHBox == null
                || dashboardForecastHighlightsVBox == null) {
            return;
        }

        try {
            ForecastOverviewService.ForecastOverviewSnapshot overview = forecastOverviewService.load(
                    HOME_ID,
                    currentRecord == null ? null : currentRecord.getTimestamp()
            );
            ForecastOverviewService.TomorrowSummarySnapshot tomorrowSummary =
                    forecastOverviewService.buildTomorrowSummary(overview);

            dashboardForecastHeadlineLabel.setText(tomorrowSummary.headline());
            dashboardForecastStatusLabel.setText("Next forecast windows");
            renderForecastBars(overview.upcomingWindows());
            renderSimpleBullets(
                    dashboardForecastHighlightsVBox,
                    tomorrowSummary.items(),
                    "Forecast details will appear once cached forecast data is ready."
            );
        } catch (RuntimeException ex) {
            dashboardForecastHeadlineLabel.setText("Forecast data is still loading.");
            dashboardForecastStatusLabel.setText("We’ll show tomorrow’s best windows once the cache refreshes.");
            dashboardQuickForecastBarHBox.getChildren().clear();
            Label fallbackLabel = new Label("No forecast windows available yet.");
            fallbackLabel.getStyleClass().add("recommendation-empty-state");
            dashboardQuickForecastBarHBox.getChildren().add(fallbackLabel);
            dashboardForecastHighlightsVBox.getChildren().clear();
        }
    }

    private void renderForecastBars(List<ForecastWindowRecord> upcomingWindows) {
        if (dashboardQuickForecastBarHBox == null) {
            return;
        }

        dashboardQuickForecastBarHBox.getChildren().clear();

        List<ForecastWindowRecord> windows = upcomingWindows == null ? List.of() : upcomingWindows.stream()
                .filter(window -> window.windowStart() != null)
                .limit(HOME_FORECAST_WINDOWS)
                .toList();

        if (windows.isEmpty()) {
            Label fallbackLabel = new Label("No forecast windows available yet.");
            fallbackLabel.getStyleClass().add("recommendation-empty-state");
            dashboardQuickForecastBarHBox.getChildren().add(fallbackLabel);
            return;
        }

        double maxSolar = windows.stream()
                .map(ForecastWindowRecord::solarShortwaveWm2)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
        double minTariff = windows.stream()
                .map(ForecastWindowRecord::tariffPPerKwh)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0.0);
        double maxTariff = windows.stream()
                .map(ForecastWindowRecord::tariffPPerKwh)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(minTariff);

        for (ForecastWindowRecord window : windows) {
            dashboardQuickForecastBarHBox.getChildren().add(
                    createForecastBar(window, maxSolar, minTariff, maxTariff)
            );
        }
    }

    private VBox createForecastBar(
            ForecastWindowRecord window,
            double maxSolar,
            double minTariff,
            double maxTariff
    ) {
        VBox container = new VBox(6);
        container.getStyleClass().add("dashboard-forecast-column");
        container.setFillWidth(true);
        HBox.setHgrow(container, Priority.ALWAYS);

        Label timeLabel = new Label(window.windowStart().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLabel.getStyleClass().add("dashboard-forecast-time");

        StackPane barFrame = new StackPane();
        barFrame.getStyleClass().add("dashboard-forecast-bar-frame");
        barFrame.setPrefHeight(84);

        Region bar = new Region();
        bar.getStyleClass().add("dashboard-forecast-bar");
        double barHeight = computeForecastBarHeight(window, maxSolar, minTariff, maxTariff);
        bar.setPrefHeight(barHeight);
        StackPane.setAlignment(bar, javafx.geometry.Pos.BOTTOM_CENTER);
        barFrame.getChildren().add(bar);

        Label detailLabel = new Label(buildForecastDetail(window));
        detailLabel.setWrapText(true);
        detailLabel.getStyleClass().add("dashboard-forecast-detail");

        container.getChildren().addAll(timeLabel, barFrame, detailLabel);
        VBox.setVgrow(barFrame, Priority.ALWAYS);
        return container;
    }

    private double computeForecastBarHeight(
            ForecastWindowRecord window,
            double maxSolar,
            double minTariff,
            double maxTariff
    ) {
        double solarScore = 0.0;
        if (window.solarShortwaveWm2() != null && maxSolar > 0.0) {
            solarScore = window.solarShortwaveWm2() / maxSolar;
        }

        double priceScore = 0.4;
        if (window.tariffPPerKwh() != null && maxTariff > minTariff) {
            priceScore = 1.0 - ((window.tariffPPerKwh() - minTariff) / (maxTariff - minTariff));
        }

        double combinedScore = Math.max(solarScore, priceScore * 0.85);
        return 18 + (Math.max(0.15, Math.min(combinedScore, 1.0)) * 54);
    }

    private String buildForecastDetail(ForecastWindowRecord window) {
        if (window.solarShortwaveWm2() != null && window.solarShortwaveWm2() > 0.0) {
            return String.format(Locale.UK, "%.0f W/m2", window.solarShortwaveWm2());
        }
        if (window.tariffPPerKwh() != null) {
            return String.format(Locale.UK, "%.1fp/kWh", window.tariffPPerKwh());
        }
        return "Forecast";
    }

    private void refreshDeviceSummary(EnergyRecord currentRecord) {
        if (dashboardDevicesOnCountLabel == null
                || dashboardDevicesScheduledCountLabel == null
                || dashboardDevicesTotalCountLabel == null
                || dashboardDevicesNextRunLabel == null
                || dashboardDevicesHighlightsVBox == null) {
            return;
        }

        List<Device> devices = deviceRepository.fetchActiveByHome(HOME_ID).stream()
                .map(CanonicalDeviceMapper::toDevice)
                .filter(device -> device != null)
                .toList();

        DashboardDeviceSnapshot snapshot = buildDeviceSnapshot(
                devices,
                currentRecord == null ? null : currentRecord.getTimestamp()
        );

        dashboardDevicesOnCountLabel.setText(Integer.toString(snapshot.onCount()));
        dashboardDevicesScheduledCountLabel.setText(Integer.toString(snapshot.scheduledCount()));
        dashboardDevicesTotalCountLabel.setText(Integer.toString(snapshot.totalCount()));
        dashboardDevicesNextRunLabel.setText(snapshot.nextRunLabel());

        renderSimpleBullets(
                dashboardDevicesHighlightsVBox,
                snapshot.highlights(),
                "Add devices to show what is running and what is scheduled."
        );
    }

    private void updateDashboardRecommendations(EnergyRecord currentRecord) {
        if (currentRecord == null) {
            renderDashboardRecommendations(new RecommendationFeedService.FeedUpdate(List.of(), List.of()));
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
        for (int index = 0; index < recommendations.size(); index++) {
            dashboardQuickRecommendationsList.getChildren().add(createRecommendationCard(recommendations.get(index), index == 0));
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
            dashboardWeeklyBudgetBudgetLabel.setText("Budget not set");
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

    private List<EnergyRecord> getAvailableHistory() {
        if (simulationService == null) {
            return List.of();
        }

        List<EnergyRecord> history = simulationService.getEnergyStatsSessionHistory();
        if (!history.isEmpty()) {
            return history;
        }

        EnergyRecord currentRecord = simulationService.getCurrentEnergyRecord().get();
        return currentRecord == null ? List.of() : List.of(currentRecord);
    }

    private void renderSimpleBullets(VBox container, List<String> items, String fallbackText) {
        if (container == null) {
            return;
        }

        container.getChildren().clear();
        List<String> content = (items == null || items.isEmpty()) ? List.of(fallbackText) : items;
        for (String item : content) {
            Label label = new Label("\u2022 " + item);
            label.setWrapText(true);
            label.getStyleClass().add("dashboard-bullet-label");
            container.getChildren().add(label);
        }
    }

    private void refreshGreeting() {
        if (dashboardGreetingLabel == null) {
            return;
        }

        dashboardGreetingLabel.setText(buildGreeting(
                homeProfileRepository.getFirstName(HOME_ID),
                LocalTime.now()
        ));
    }

    private String buildUsageInsightLine(DashboardUsageSnapshot usageSnapshot) {
        if (usageSnapshot.todayRecordCount() == 0) {
            return "Waiting for today's simulation totals";
        }

        EnergyStatsController.SourceBreakdown breakdown = usageSnapshot.summarySnapshot().sourceBreakdown();
        return String.format(
                Locale.UK,
                "%s projected today • %d%% solar, %d%% battery, %d%% grid",
                formatCurrency(usageSnapshot.projectedDailyCost()),
                breakdown.solarPercentDisplay(),
                breakdown.batteryPercentDisplay(),
                breakdown.gridPercentDisplay()
        );
    }

    private String formatCurrency(double amount) {
        return String.format("\u00A3%.2f", amount);
    }

    private void showValidationMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setHeaderText("Invalid budget value");
        alert.showAndWait();
    }

    private static String presentationStatus(Device device) {
        if (device == null) {
            return "OFF";
        }
        if (device.isOn()) {
            return "ON";
        }
        if (device instanceof ScheduledDevice scheduledDevice) {
            if (scheduledDevice.isSkipNextRun()) {
                return "SKIPPED";
            }
            if (scheduledDevice.getSchedule() != null) {
                return "SCHEDULED";
            }
        }
        return "OFF";
    }

    private static String displayNameForDevice(Device device) {
        if (device == null || device.getDeviceName() == null || device.getDeviceName().isBlank()) {
            return "Device";
        }
        return device.getDeviceName().trim();
    }

    private static LocalDateTime nextRunTime(ScheduledDevice device, LocalDateTime referenceTime) {
        LocalDateTime nextRun = referenceTime.toLocalDate().atTime(device.getSchedule());
        if (nextRun.isBefore(referenceTime)) {
            nextRun = nextRun.plusDays(1);
        }
        return nextRun;
    }

    private record DashboardUsageSnapshot(
            EnergyStatsController.SummarySnapshot summarySnapshot,
            double projectedDailyCost,
            int todayRecordCount
    ) {
    }
}
