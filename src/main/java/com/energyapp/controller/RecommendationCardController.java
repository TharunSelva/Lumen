package com.energyapp.controller;

import com.energyapp.model.Recommendation;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

public class RecommendationCardController {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM, HH:mm");

    @FXML private Label recExplanationLabel;
    @FXML private Label recTimestampLabel;
    @FXML private Label recTitleLabel;
    @FXML private VBox recommendationCardRoot;

    private Runnable onNavigate;

    @FXML
    public void initialize() {
        recTimestampLabel.setTextOverrun(OverrunStyle.CLIP);
        recTitleLabel.setTextOverrun(OverrunStyle.CLIP);
        recExplanationLabel.setTextOverrun(OverrunStyle.CLIP);
        recommendationCardRoot.setOnMouseClicked(event -> {
            if (onNavigate != null) {
                onNavigate.run();
            }
        });
    }

    public void setRecommendation(Recommendation recommendation) {
        recTitleLabel.setText(recommendation.getTitle());
        recExplanationLabel.setText(recommendation.getExplanation());
        recTimestampLabel.setText(recommendation.getShownAt() == null
                ? "Shown recently"
                : "Shown " + recommendation.getShownAt().format(TIME_FORMAT));

        recommendationCardRoot.getStyleClass().removeAll("recommendation-card-active", "recommendation-card-history");
        recommendationCardRoot.getStyleClass().add(recommendation.isActive()
                ? "recommendation-card-active"
                : "recommendation-card-history");
    }

    public void setOnNavigate(Runnable onNavigate) {
        this.onNavigate = onNavigate;
        recommendationCardRoot.setCursor(onNavigate == null ? Cursor.DEFAULT : Cursor.HAND);
    }

}
