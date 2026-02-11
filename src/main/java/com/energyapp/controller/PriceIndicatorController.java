package com.energyapp.controller;

import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class PriceIndicatorController {
    private final Label label;
    private final Circle circle;

    public PriceIndicatorController(Label label, Circle circle) {
        this.label = label;
        this.circle = circle;
    }

    public void updateIndicatorByCost(double intervalCost) {
        if (intervalCost < 0.05) {
            updateUI(Color.web("#bbff9c"), "Low Price", Color.GREEN);
        } else if (intervalCost < 0.15) {
            updateUI(Color.ORANGE, "Standard Price", Color.DARKORANGE);
        } else {
            updateUI(Color.RED, "High Price", Color.DARKRED);
        }
    }

    private void updateUI(Color color, String text, Color textColor) {
        circle.setFill(color);
        label.setText(text);
        label.setTextFill(textColor);
    }
}