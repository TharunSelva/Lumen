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

    @Test
    void testUpdateIndicator_StandardUsage_Orange() throws InterruptedException {
        Label mockLabel = new Label();
        Circle mockCircle = new Circle();
        PriceIndicatorController controller = new PriceIndicatorController(mockLabel, mockCircle);

        runOnFxThread(() -> {
            // 0.10 is >= 0.05 and < 0.15, so "Standard Usage"
            controller.updateIndicatorByCost(0.10);
        });

        assertEquals("Standard Usage", mockLabel.getText());
        assertEquals(Color.DARKORANGE, mockLabel.getTextFill());
        assertEquals(Color.ORANGE, mockCircle.getFill());
    }

    @Test
    void testUpdateIndicator_LowBoundary() throws InterruptedException {
        Label mockLabel = new Label();
        Circle mockCircle = new Circle();
        PriceIndicatorController controller = new PriceIndicatorController(mockLabel, mockCircle);

        runOnFxThread(() -> {
            // Exactly at 0.05 should be standard usage
            controller.updateIndicatorByCost(0.05);
        });

        assertEquals("Standard Usage", mockLabel.getText());
    }

    @Test
    void testUpdateIndicator_HighBoundary() throws InterruptedException {
        Label mockLabel = new Label();
        Circle mockCircle = new Circle();
        PriceIndicatorController controller = new PriceIndicatorController(mockLabel, mockCircle);

        runOnFxThread(() -> {
            // Exactly at 0.15 should be high usage
            controller.updateIndicatorByCost(0.15);
        });

        assertEquals("High Usage", mockLabel.getText());
    }

    @Test
    void testUpdateIndicator_ZeroCost() throws InterruptedException {
        Label mockLabel = new Label();
        Circle mockCircle = new Circle();
        PriceIndicatorController controller = new PriceIndicatorController(mockLabel, mockCircle);

        runOnFxThread(() -> {
            controller.updateIndicatorByCost(0.0);
        });

        assertEquals("Low Usage", mockLabel.getText());
        assertEquals(Color.GREEN, mockLabel.getTextFill());
    }

    @Test
    void testUpdateIndicator_VeryHighCost() throws InterruptedException {
        Label mockLabel = new Label();
        Circle mockCircle = new Circle();
        PriceIndicatorController controller = new PriceIndicatorController(mockLabel, mockCircle);

        runOnFxThread(() -> {
            controller.updateIndicatorByCost(1.0);
        });

        assertEquals("High Usage", mockLabel.getText());
        assertEquals(Color.DARKRED, mockLabel.getTextFill());
        assertEquals(Color.RED, mockCircle.getFill());
    }

    @Test
    void testUpdateIndicator_VeryLowCost() throws InterruptedException {
        Label mockLabel = new Label();
        Circle mockCircle = new Circle();
        PriceIndicatorController controller = new PriceIndicatorController(mockLabel, mockCircle);

        runOnFxThread(() -> {
            controller.updateIndicatorByCost(0.001);
        });

        assertEquals("Low Usage", mockLabel.getText());
        assertEquals(Color.GREEN, mockLabel.getTextFill());
    }

    @Test
    void testUpdateIndicator_JustBelowStandard() throws InterruptedException {
        Label mockLabel = new Label();
        Circle mockCircle = new Circle();
        PriceIndicatorController controller = new PriceIndicatorController(mockLabel, mockCircle);

        runOnFxThread(() -> {
            controller.updateIndicatorByCost(0.049);
        });

        assertEquals("Low Usage", mockLabel.getText());
    }

    @Test
    void testUpdateIndicator_JustBelowHigh() throws InterruptedException {
        Label mockLabel = new Label();
        Circle mockCircle = new Circle();
        PriceIndicatorController controller = new PriceIndicatorController(mockLabel, mockCircle);

        runOnFxThread(() -> {
            controller.updateIndicatorByCost(0.149);
        });

        assertEquals("Standard Usage", mockLabel.getText());
    }

    @Test
    void testMultipleUpdates() throws InterruptedException {
        Label mockLabel = new Label();
        Circle mockCircle = new Circle();
        PriceIndicatorController controller = new PriceIndicatorController(mockLabel, mockCircle);

        runOnFxThread(() -> {
            controller.updateIndicatorByCost(0.02);
        });
        assertEquals("Low Usage", mockLabel.getText());

        runOnFxThread(() -> {
            controller.updateIndicatorByCost(0.10);
        });
        assertEquals("Standard Usage", mockLabel.getText());

        runOnFxThread(() -> {
            controller.updateIndicatorByCost(0.25);
        });
        assertEquals("High Usage", mockLabel.getText());

        runOnFxThread(() -> {
            controller.updateIndicatorByCost(0.03);
        });
        assertEquals("Low Usage", mockLabel.getText());
    }

    @Test
    void testCircleColorChangesWithLevel() throws InterruptedException {
        Label mockLabel = new Label();
        Circle mockCircle = new Circle();
        PriceIndicatorController controller = new PriceIndicatorController(mockLabel, mockCircle);

        runOnFxThread(() -> {
            controller.updateIndicatorByCost(0.02);
        });
        assertEquals(Color.web("#bbff9c"), mockCircle.getFill());

        runOnFxThread(() -> {
            controller.updateIndicatorByCost(0.10);
        });
        assertEquals(Color.ORANGE, mockCircle.getFill());

        runOnFxThread(() -> {
            controller.updateIndicatorByCost(0.20);
        });
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