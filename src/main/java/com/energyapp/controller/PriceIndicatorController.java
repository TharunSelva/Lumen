package com.energyapp.controller;

import com.energyapp.utils.EnergyCalculator;
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
        String usageLevel = EnergyCalculator.determineEnergyUsageLevel(intervalCost);
        // depending on usage level, switch out colors on indicator
        switch (usageLevel) {
            case "Low Usage":
                updateUI(Color.web("#bbff9c"), usageLevel, Color.GREEN);
                break;
            case "Standard Usage":
                updateUI(Color.ORANGE, usageLevel, Color.DARKORANGE);
                break;
            case "High Usage":
                updateUI(Color.RED, usageLevel, Color.DARKRED);
                break;
            default:
                updateUI(Color.GREY, "Unknown", Color.BLACK);
        }
    }

    private void updateUI(Color color, String text, Color textColor) {
        circle.setFill(color);
        label.setText(text);
        label.setTextFill(textColor);
    }
}