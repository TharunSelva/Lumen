package com.energyapp.controller;

import com.energyapp.db.DbPaths;
import com.energyapp.db.SqlScriptRunner;
import com.energyapp.db.Sqlite;
import com.energyapp.model.Dishwasher;
import com.energyapp.model.EnergyRecord;
import com.energyapp.model.WashingMachine;
import com.energyapp.service.SimulationService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.shape.Circle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DashboardControllerTest {

    private String originalUserHome;

    @BeforeAll
    static void initJfx() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {}
    }

    @BeforeEach
    void setUp() throws Exception {
        originalUserHome = System.getProperty("user.home");
        Path testHome = Path.of(System.getProperty("java.io.tmpdir"), "lumen-dashboard-test-home");
        System.setProperty("user.home", testHome.toString());

        DbPaths.ensureDataDirExists();
        try (Connection conn = Sqlite.openConnection()) {
            SqlScriptRunner.runResource(conn, "/com/schema.sql");
        }
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.home", originalUserHome);
    }

    @Test
    void testSetSimService_InitializesComponents() throws Exception {
        DashboardController dashboard = new DashboardController();

        inject(dashboard, "dashboardUsageXAxis", new NumberAxis());
        inject(dashboard, "dashboardUsageLineChart", new LineChart<>(new NumberAxis(), new NumberAxis()));

        Label spyLabel = new Label();
        PriceIndicatorController spyIndicator = new PriceIndicatorController(spyLabel, new Circle());
        inject(dashboard, "indicatorLogic", spyIndicator);

        SimulationService mockService = new SimulationService();
        EnergyRecord dummyRecord = new EnergyRecord(
                LocalDateTime.now(), "12:00", 10.0, 50.0, 0, 0, 0, 0, 0, 0, 0, 0
        );

        Field recordField = SimulationService.class.getDeclaredField("currentEnergyRecord");
        recordField.setAccessible(true);
        ((SimpleObjectProperty<EnergyRecord>) recordField.get(mockService)).set(dummyRecord);

        Platform.runLater(() -> dashboard.setSimService(mockService));
        Thread.sleep(500);

        assertEquals("High Usage", spyLabel.getText(),
                "Dashboard should update the price indicator immediately upon connecting service");
    }

    @Test
    void testRefreshWeeklyBudgetDisplay_ShowsOverBudgetSummaryAndWeeklyCost() throws Exception {
        DashboardController dashboard = new DashboardController();
        SimulationService service = new SimulationService();

        setField(service, "weeklyAccumulatedCost", 5.55);

        inject(dashboard, "simulationService", service);
        inject(dashboard, "dashboardWeeklyBudgetSpentLabel", new Label());
        inject(dashboard, "dashboardWeeklyBudgetBudgetLabel", new Label());
        inject(dashboard, "dashboardWeeklyBudgetRemainingLabel", new Label());
        inject(dashboard, "dashboardWeeklyBudgetStatusLabel", new Label());
        inject(dashboard, "dashboardWeeklyBudgetHelperLabel", new Label());
        inject(dashboard, "dashboardWeeklyBudgetProgressBar", new ProgressBar());
        inject(dashboard, "dashboardWeeklyBudgetEditButton", new Button());

        CostCardController costCardController = new CostCardController();
        inject(costCardController, "costCardLabel", new Label());
        inject(dashboard, "weeklyCostCardController", costCardController);
        inject(dashboard, "currentWeeklyBudgetGbp", 4.60);

        Method refreshMethod = DashboardController.class.getDeclaredMethod("refreshWeeklyBudgetDisplay");
        refreshMethod.setAccessible(true);

        runOnFxThread(() -> {
            try {
                refreshMethod.invoke(dashboard);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        Label spentLabel = (Label) getField(dashboard, "dashboardWeeklyBudgetSpentLabel");
        Label budgetLabel = (Label) getField(dashboard, "dashboardWeeklyBudgetBudgetLabel");
        Label remainingLabel = (Label) getField(dashboard, "dashboardWeeklyBudgetRemainingLabel");
        Label statusLabel = (Label) getField(dashboard, "dashboardWeeklyBudgetStatusLabel");
        ProgressBar progressBar = (ProgressBar) getField(dashboard, "dashboardWeeklyBudgetProgressBar");
        Label costCardLabel = (Label) getField(costCardController, "costCardLabel");

        assertEquals("\u00A35.55 spent", spentLabel.getText());
        assertEquals("Budget \u00A34.60", budgetLabel.getText());
        assertEquals("Over by \u00A30.95", remainingLabel.getText());
        assertEquals("Over budget", statusLabel.getText());
        assertTrue(statusLabel.getStyleClass().contains("budget-status-over"));
        assertTrue(progressBar.getStyleClass().contains("budget-progress-over"));
        assertEquals(1.0, progressBar.getProgress(), 0.001);
        assertEquals("\u00A35.55 so far this week...", costCardLabel.getText());
    }

    @Test
    void buildGreeting_UsesStoredFirstNameAndTimeOfDay() {
        assertEquals("Good morning, Priya!", DashboardController.buildGreeting("Priya", LocalTime.of(9, 0)));
        assertEquals("Good afternoon!", DashboardController.buildGreeting(" ", LocalTime.of(15, 0)));
        assertEquals("Good evening, Sam!", DashboardController.buildGreeting("Sam", LocalTime.of(20, 30)));
    }

    @Test
    void calculateProjectedDailyCost_UsesHalfHourIntervals() {
        assertEquals(12.0, DashboardController.calculateProjectedDailyCost(3.0, 12), 0.001);
        assertEquals(0.0, DashboardController.calculateProjectedDailyCost(3.0, 0), 0.001);
    }

    @Test
    void buildDeviceSnapshot_SummarisesOnDevicesAndNextRun() {
        WashingMachine washer = new WashingMachine("Laundry Washer");
        washer.setOn(true);
        washer.setSchedule(LocalTime.of(18, 0));

        Dishwasher dishwasher = new Dishwasher("Kitchen Dishwasher");
        dishwasher.setSchedule(LocalTime.of(13, 30));

        DashboardController.DashboardDeviceSnapshot snapshot = DashboardController.buildDeviceSnapshot(
                List.of(washer, dishwasher),
                LocalDateTime.of(2026, 4, 24, 12, 0)
        );

        assertEquals(1, snapshot.onCount());
        assertEquals(1, snapshot.scheduledCount());
        assertEquals(2, snapshot.totalCount());
        assertEquals("Next run Fri 13:30", snapshot.nextRunLabel());
        assertTrue(snapshot.highlights().stream().anyMatch(item -> item.contains("On now: Laundry Washer")));
    }

    @Test
    void dashboardViewLoadsSuccessfully() throws Exception {
        CountDownLatch loadLatch = new CountDownLatch(1);
        Parent[] loadedRoot = new Parent[1];

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/energyapp/view/Dashboard.fxml"));
                loadedRoot[0] = loader.load();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                loadLatch.countDown();
            }
        });

        assertTrue(loadLatch.await(2, TimeUnit.SECONDS));
        assertTrue(loadedRoot[0] != null);
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        inject(target, fieldName, value);
    }

    private Object getField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    private void runOnFxThread(Runnable action) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            action.run();
            latch.countDown();
        });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }
}
