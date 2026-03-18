package com.energyapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class InsightCardController {
    @FXML private Label insightLabel;
    @FXML private Rectangle statusIcon;

    public void setContent(String text, Color color) {
        if (insightLabel != null) {
            insightLabel.setText(text);
        }
        if (statusIcon != null) {
            statusIcon.setFill(color);
        }
    }
}