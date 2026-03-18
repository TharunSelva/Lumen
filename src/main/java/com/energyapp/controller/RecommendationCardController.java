package com.energyapp.controller;

import com.energyapp.model.Recommendation;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class RecommendationCardController {

    @FXML private Label recExplanationLabel;
    @FXML private Label recTitleLabel;

    public void setRecommendation(Recommendation recommendation) {
        recTitleLabel.setText(recommendation.getTitle());
        recExplanationLabel.setText(recommendation.getExplanation());
    }

}
