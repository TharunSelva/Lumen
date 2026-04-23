package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.shape.Circle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class DashboardControllerTest {

    @BeforeAll
    static void initJfx() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException e) {}
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

        assertEquals("High Usage", spyLabel.getText(), "Dashboard should update the price indicator immediately upon connecting service");
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

        assertEquals("£5.55 spent", spentLabel.getText());
        assertEquals("Budget £4.60", budgetLabel.getText());
        assertEquals("Over by £0.95", remainingLabel.getText());
        assertEquals("Over budget", statusLabel.getText());
        assertTrue(statusLabel.getStyleClass().contains("budget-status-over"));
        assertTrue(progressBar.getStyleClass().contains("budget-progress-over"));
        assertEquals(1.0, progressBar.getProgress(), 0.001);
        assertEquals("£5.55 so far this week...", costCardLabel.getText());
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
