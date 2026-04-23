package com.energyapp.controller;

import com.energyapp.service.QuickTipService;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class QuickTipMarqueeController {

    private static final double PIXELS_PER_SECOND = 70.0;
    private static final String LOOP_GAP = "      |      ";

    @FXML private StackPane marqueeViewport;
    @FXML private Label marqueeLabel;

    private final QuickTipService quickTipService = QuickTipService.getInstance();
    private TranslateTransition activeTransition;
    private String baseText;

    @FXML
    public void initialize() {
        marqueeLabel.setManaged(false);
        marqueeLabel.setMinWidth(Region.USE_PREF_SIZE);
        marqueeLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);
        marqueeLabel.setMaxWidth(Region.USE_PREF_SIZE);
        marqueeLabel.setWrapText(false);
        marqueeLabel.setTextOverrun(OverrunStyle.CLIP);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(marqueeViewport.widthProperty());
        clip.heightProperty().bind(marqueeViewport.heightProperty());
        marqueeViewport.setClip(clip);

        marqueeViewport.widthProperty().addListener((obs, oldWidth, newWidth) -> restartAnimation());

        Platform.runLater(() -> {
            baseText = quickTipService.getContinuousMarqueeText();
            restartAnimation();
        });
    }

    private void restartAnimation() {
        if (baseText == null || baseText.isBlank()) {
            baseText = quickTipService.getContinuousMarqueeText();
        }

        double viewportWidth = marqueeViewport.getWidth();
        if (viewportWidth <= 0) {
            return;
        }

        if (activeTransition != null) {
            activeTransition.stop();
        }

        Text cycleMeasure = new Text(baseText + LOOP_GAP);
        cycleMeasure.setFont(marqueeLabel.getFont());
        double cycleWidth = cycleMeasure.getLayoutBounds().getWidth();
        if (cycleWidth <= 0) {
            return;
        }

        marqueeLabel.setText(baseText + LOOP_GAP + baseText);
        marqueeLabel.applyCss();
        marqueeLabel.autosize();

        if (cycleWidth <= viewportWidth) {
            marqueeLabel.setTranslateX(0);
            return;
        }

        marqueeLabel.setTranslateX(0);
        double durationSeconds = cycleWidth / PIXELS_PER_SECOND;
        activeTransition = new TranslateTransition(Duration.seconds(durationSeconds), marqueeLabel);
        activeTransition.setInterpolator(Interpolator.LINEAR);
        activeTransition.setFromX(0);
        activeTransition.setToX(-cycleWidth);
        activeTransition.setCycleCount(TranslateTransition.INDEFINITE);
        activeTransition.play();
    }
}
