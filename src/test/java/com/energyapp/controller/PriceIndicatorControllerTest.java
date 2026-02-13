package com.energyapp.controller;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class PriceIndicatorControllerTest {
    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
        }
    }

    @Test
    void testUpdateIndicator_LowUsage_Green() throws InterruptedException {
        Label mockLabel = new Label();
        Circle mockCircle = new Circle();
        PriceIndicatorController controller = new PriceIndicatorController(mockLabel, mockCircle);

        runOnFxThread(() -> {
            // 0.04 is < 0.05, so "Low Usage"
            controller.updateIndicatorByCost(0.04);
        });

        assertEquals("Low Usage", mockLabel.getText());
        assertEquals(Color.GREEN, mockLabel.getTextFill());

        assertEquals(Color.web("#bbff9c"), mockCircle.getFill());
    }

    @Test
    void testUpdateIndicator_HighUsage_Red() throws InterruptedException {
        Label mockLabel = new Label();
        Circle mockCircle = new Circle();
        PriceIndicatorController controller = new PriceIndicatorController(mockLabel, mockCircle);

        runOnFxThread(() -> {
            controller.updateIndicatorByCost(0.20);
        });

        assertEquals("High Usage", mockLabel.getText());
        assertEquals(Color.DARKRED, mockLabel.getTextFill());
        assertEquals(Color.RED, mockCircle.getFill());
    }

    private void runOnFxThread(Runnable action) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            action.run();
            latch.countDown();
        });
        latch.await(1, TimeUnit.SECONDS);
    }
}