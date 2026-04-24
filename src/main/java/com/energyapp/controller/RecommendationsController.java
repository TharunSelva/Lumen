package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import com.energyapp.model.Recommendation;
import com.energyapp.service.RecommendationFeedService;
import com.energyapp.service.RecommendationTopSectionService;
import com.energyapp.service.SimulationService;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecommendationsController {

    private static final int PAGE_VISIBLE_RECOMMENDATIONS = 5;

    @FXML
    private VBox recommendationsContainer;
    @FXML
    private StackPane recommendationsFeedViewport;
    @FXML
    private Label priorityRecommendationTitleLabel;
    @FXML
    private Label priorityRecommendationSummaryLabel;
    @FXML
    private Label priorityRecommendationWhyLabel;
    @FXML
    private Label priorityRecommendationImpactLabel;
    @FXML
    private javafx.scene.control.Button priorityRecommendationActionButton;
    @FXML
    private VBox smartSavingsList;
    @FXML
    private VBox solarOpportunitiesList;

    private MainLayoutController mainController;
    private SimulationService simulationService;
    private final RecommendationFeedService recommendationFeedService = RecommendationFeedService.getInstance();
    private final RecommendationTopSectionService recommendationTopSectionService = new RecommendationTopSectionService();
    private RecommendationTopSectionService.NavigationTarget priorityNavigationTarget =
            RecommendationTopSectionService.NavigationTarget.FORECASTS;

    @FXML
    public void initialize() {
        if (recommendationsFeedViewport != null) {
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(recommendationsFeedViewport.widthProperty());
            clip.heightProperty().bind(recommendationsFeedViewport.heightProperty());
            recommendationsFeedViewport.setClip(clip);
        }
    }

    public void setSimulationService(SimulationService service) {
        this.simulationService = service;
        updateRecommendationsUI(this.simulationService.getCurrentEnergyRecord().get());

        this.simulationService.getCurrentEnergyRecord().addListener((
                obs, oldRec, newRec) -> {
            updateRecommendationsUI(newRec);
        });
    }

    public void setMainController(MainLayoutController mainController) {
        this.mainController = mainController;
    }

    private void updateRecommendationsUI(EnergyRecord currentRecord) {
        // as in other parts of the app, MUST use runLater as sim service on background thread.
        Platform.runLater(() -> {
            if (recommendationsContainer == null) {
                return;
            }

            renderTopSections(recommendationTopSectionService.build(currentRecord));

            RecommendationFeedService.FeedUpdate feedUpdate = recommendationFeedService.updateRecommendations(currentRecord);
            List<Recommendation> allRecommendations = feedUpdate.getVisibleRecommendations();
            List<Recommendation> activeRecommendations = allRecommendations.size() <= PAGE_VISIBLE_RECOMMENDATIONS
                    ? allRecommendations
                    : allRecommendations.subList(allRecommendations.size() - PAGE_VISIBLE_RECOMMENDATIONS, allRecommendations.size());
            activeRecommendations = newestFirst(activeRecommendations);

            if (activeRecommendations.isEmpty()) {
                recommendationsContainer.getChildren().clear();
                Label emptyState = new Label("No live recommendations just yet. Check back as your energy data updates.");
                emptyState.getStyleClass().add("recommendation-empty-state");
                emptyState.setWrapText(true);
                recommendationsContainer.getChildren().add(emptyState);
                return;
            }

            if (recommendationsContainer.getChildren().isEmpty() || feedUpdate.insertedCount() != 1) {
                replaceRecommendationCards(activeRecommendations);
                return;
            }

            Recommendation insertedRecommendation = activeRecommendations.stream()
                    .filter(recommendation -> feedUpdate.isNewlyInserted(recommendation.getId()))
                    .findFirst()
                    .orElse(null);

            if (insertedRecommendation == null) {
                replaceRecommendationCards(activeRecommendations);
                return;
            }

            if (recommendationsContainer.getChildren().size() < RecommendationFeedService.MAX_VISIBLE_RECOMMENDATIONS) {
                Region cardNode = createRecommendationCard(insertedRecommendation);
                animateNewTopCard(cardNode);
                return;
            }

            animatePushDownTransition(insertedRecommendation, activeRecommendations);
        });
    }

    @FXML
    private void handlePriorityAction() {
        if (mainController == null) {
            return;
        }

        switch (priorityNavigationTarget) {
            case DEVICES -> mainController.handleShowDevices();
            case FORECASTS -> mainController.handleShowForecasting();
        }
    }

    private void renderTopSections(RecommendationTopSectionService.TopSectionsSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        RecommendationTopSectionService.PriorityCard priorityCard = snapshot.priorityCard();
        priorityRecommendationTitleLabel.setText(priorityCard.title());
        priorityRecommendationSummaryLabel.setText(priorityCard.summary());
        priorityRecommendationWhyLabel.setText(priorityCard.why());
        priorityRecommendationImpactLabel.setText(priorityCard.impact());
        priorityRecommendationActionButton.setText(priorityCard.actionLabel());
        priorityNavigationTarget = priorityCard.navigationTarget();

        populateSectionItems(smartSavingsList, snapshot.tonightSection().items());
        populateSectionItems(solarOpportunitiesList, snapshot.tomorrowSection().items());
    }

    private void populateSectionItems(VBox container, List<String> items) {
        container.getChildren().clear();
        for (String item : items) {
            Label label = new Label(item);
            label.setWrapText(true);
            label.setMaxWidth(Double.MAX_VALUE);
            label.getStyleClass().add("recommendations-list-item");
            container.getChildren().add(label);
        }
    }

    private void replaceRecommendationCards(List<Recommendation> recommendations) {
        recommendationsContainer.getChildren().clear();
        for (int i = 0; i < recommendations.size(); i++) {
            recommendationsContainer.getChildren().add(createRecommendationCard(recommendations.get(i), i == 0));
        }
    }

    private Region createRecommendationCard(Recommendation recommendation) {
        return createRecommendationCard(recommendation, false);
    }

    private Region createRecommendationCard(Recommendation recommendation, boolean highlightLatest) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/energyapp/view/RecommendationCard.fxml"));
            Region cardNode = loader.load();
            RecommendationCardController controller = loader.getController();
            controller.setRecommendation(recommendation);
            controller.setOnNavigate(null);
            cardNode.getStyleClass().remove("recommendation-card-latest");
            if (highlightLatest) {
                cardNode.getStyleClass().add("recommendation-card-latest");
            }
            cardNode.setUserData(recommendation.getId());
            return cardNode;
        } catch (IOException e) {
            throw new RuntimeException("Failed to render recommendation card", e);
        }
    }

    private void animateNewTopCard(Region cardNode) {
        recommendationsContainer.getChildren().add(0, cardNode);
        cardNode.applyCss();
        cardNode.layout();
        cardNode.setOpacity(0);
        cardNode.setTranslateY(-16);

        FadeTransition fade = new FadeTransition(Duration.millis(220), cardNode);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(220), cardNode);
        slide.setFromY(-16);
        slide.setToY(0);

        new ParallelTransition(fade, slide).play();
    }

    private void animatePushDownTransition(Recommendation insertedRecommendation, List<Recommendation> finalRecommendations) {
        Region newCard = createRecommendationCard(insertedRecommendation, true);
        recommendationsContainer.getChildren().add(0, newCard);
        recommendationsContainer.applyCss();
        recommendationsContainer.layout();

        double cardHeight = newCard.getBoundsInParent().getHeight();
        double shiftAmount = cardHeight + recommendationsContainer.getSpacing();
        Region bottomCard = (Region) recommendationsContainer.getChildren().get(recommendationsContainer.getChildren().size() - 1);

        FadeTransition fadeBottom = new FadeTransition(Duration.millis(320), bottomCard);
        fadeBottom.setFromValue(1);
        fadeBottom.setToValue(0);

        TranslateTransition pushDown = new TranslateTransition(Duration.millis(420), recommendationsContainer);
        pushDown.setFromY(-shiftAmount);
        pushDown.setToY(0);
        pushDown.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);

        ParallelTransition transition = new ParallelTransition(fadeBottom, pushDown);
        transition.setOnFinished(event -> {
            recommendationsContainer.setTranslateY(0);
            replaceRecommendationCards(finalRecommendations);
        });
        transition.play();
    }

    private List<Recommendation> newestFirst(List<Recommendation> recommendations) {
        List<Recommendation> ordered = new ArrayList<>(recommendations);
        Collections.reverse(ordered);
        return ordered;
    }
}


