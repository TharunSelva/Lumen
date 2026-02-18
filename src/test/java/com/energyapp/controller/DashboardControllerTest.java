package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

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

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }
}