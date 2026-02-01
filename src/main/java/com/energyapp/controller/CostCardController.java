package com.energyapp.controller;


import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class CostCardController {

    @FXML private Label costCardLabel;

    public void setCost(double cost) {
        costCardLabel.setText(String.format("£%.2f so far this week...", cost));
    }

    public void initialize() {
        System.out.println("CostCardController successfully loaded.");
    }

}
