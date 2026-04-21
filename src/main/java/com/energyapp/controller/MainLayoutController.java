package com.energyapp.controller;

import com.energyapp.db.HomeProfileRepository;
import com.energyapp.model.Geocode;
import com.energyapp.model.ViewType;
import com.energyapp.service.AccessibilityManager;
import com.energyapp.service.SimulationService;
import com.energyapp.service.TextToSpeechService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

import static javafx.application.Platform.runLater;

public class MainLayoutController {
    @FXML private BorderPane rootLayout;
    @FXML private SideBarController mainSideBarController;

    private SimulationService simulationService;
    private ViewType currentView;
    private TextToSpeechService ttsService = new TextToSpeechService();
    private final HomeProfileRepository homeProfileRepository = new HomeProfileRepository();

    public void initialize() {
        if (mainSideBarController != null) {
            mainSideBarController.setMainController(this);
        }

        AccessibilityManager.isLargeText().addListener((obs, wasLarge, isLarge) -> {
            if (isLarge) {
                if (!rootLayout.getStyleClass().contains("large-text")) {
                    rootLayout.getStyleClass().add("large-text");
                }
            } else {
                rootLayout.getStyleClass().remove("large-text");
            }
        });

        AccessibilityManager.isCompactView().addListener((obs, wasCompact, isCompact) -> {
            Stage stage = (Stage) rootLayout.getScene().getWindow();
            if (isCompact) {
                stage.setWidth(700.0);
                stage.setHeight(700.0);
                Node center = rootLayout.getCenter();
                if (center != null) {
                    applyCompactToHierarchy(center);
                }
                stage.setResizable(false);
                if (!rootLayout.getStyleClass().contains("compact-view")) {
                    rootLayout.getStyleClass().add("compact-view");
                }
            } else {
                stage.setWidth(1280.0);
                stage.setHeight(720.0);
                rootLayout.getStyleClass().remove("compact-view");
                stage.setResizable(true);
                switchView(currentView);
            }
        });

        AccessibilityManager.customFontSizeProperty().addListener((obs, oldVal, newVal) -> applyGlobalTextSettings());
        AccessibilityManager.customFontFamilyProperty().addListener((obs, oldVal, newVal) -> applyGlobalTextSettings());
        AccessibilityManager.customFontBoldProperty().addListener((obs, oldVal, newVal) -> applyGlobalTextSettings());

        applyGlobalTextSettings();
        checkAndShowSetup();

    }

    private void switchView (ViewType viewType) {
        this.currentView = viewType;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(viewType.getFxmlPath()));
            Parent root = loader.load();

            Object controller = loader.getController();

            if (controller instanceof DashboardController) {
                DashboardController dc = (DashboardController) controller;
                dc.setMainController(this);
                dc.setSimService(this.simulationService);
            } else if (controller instanceof EnergyStatsController) {
                ((EnergyStatsController) controller).setSimulationService(this.simulationService);
            } else if (controller instanceof RecommendationsController) {
                ((RecommendationsController) controller).setMainController(this);
                ((RecommendationsController) controller).setSimulationService(this.simulationService);
            } else if (controller instanceof EcoStatisticsController) {
                ((EcoStatisticsController) controller).setSimulationService(this.simulationService);
            } else if (controller instanceof ForecastingController) {
                    ((ForecastingController) controller).setSimulationService(this.simulationService);
            } else if (controller instanceof DevicesController) {
                ((DevicesController) controller).setSimulationService(this.simulationService);
            }

            if (AccessibilityManager.isCompactView().get()) {
                runLater(() -> applyCompactToHierarchy(root));
            }
        rootLayout.setCenter(root);
        autoFormatTitles(root);
        attachScreenReader(root);
        } catch(IOException e) {
            e.printStackTrace();
        }

        if (rootLayout.getLeft() != null) {
            attachScreenReader(rootLayout.getLeft());
        }
    }


    @FXML
    public void handleShowDashboard() {
        switchView(ViewType.DASHBOARD);
    }

    @FXML
    public void handleShowDevices() {
        switchView(ViewType.DEVICES);
    }

    @FXML
    public void handleShowStatistics() {
        switchView(ViewType.ENERGYSTATISTICS);
    }

    @FXML
    public void handleShowSettings() {
        switchView(ViewType.SETTINGS);
    }

    public void setSimService (SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    public void handleShowForecasting() {
        switchView(ViewType.FORECASTING);
    }

    public void handleShowRecommendations () {
        switchView(ViewType.RECOMMENDATIONS);
    }

    public void handleShowEcoStatistics() {
        switchView(ViewType.ECOIMPACT);
    }

    private void applyCompactToHierarchy(Node node) {
        if (node instanceof GridPane gp) {
            reflowGridPane(gp);
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyCompactToHierarchy(child);
            }
        }

        if (node instanceof javafx.scene.control.ScrollPane sp && sp.getContent() != null) {
            applyCompactToHierarchy(sp.getContent());
        }
    }

    private void reflowGridPane(GridPane gp) {
        gp.getColumnConstraints().clear();
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(100);
        gp.getColumnConstraints().add(cc);

        int nextAvailableRow = 0;

        for (Node child : gp.getChildren()) {
            if (child.isManaged() && child.isVisible()) {
                GridPane.setRowIndex(child, nextAvailableRow);
                GridPane.setColumnIndex(child, 0);
                nextAvailableRow++;
            }
        }
    }

    private void applyGlobalTextSettings() {
        double size = AccessibilityManager.customFontSizeProperty().get();
        String family = AccessibilityManager.customFontFamilyProperty().get();
        boolean isBold = AccessibilityManager.customFontBoldProperty().get();

        String weight = isBold ? "bold" : "normal";

        String cssStyle = String.format("-fx-font-size: %.1fpx; -fx-font-family: \"%s\"; -fx-font-weight: %s;",
                size, family, weight);

        rootLayout.setStyle(cssStyle);
    }

    private void autoFormatTitles(Node node) {
        if (node instanceof javafx.scene.control.Label lbl) {
            try {
                if (lbl.getFont() != null && lbl.getFont().getSize() > 18.0 && !lbl.getId().equals("costCardLabel") && !lbl.getId().equals("priceStatusText")) {
                    lbl.setStyle("");
                    if (!lbl.getStyleClass().contains("screen-title")) {
                        lbl.getStyleClass().add("screen-title");
                    }
                }
            } catch (NullPointerException ignored) {

            }
        }

        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                autoFormatTitles(child);
            }
        }

        if (node instanceof javafx.scene.control.ScrollPane sp && sp.getContent() != null) {
            autoFormatTitles(sp.getContent());
        }
    }

    private void attachScreenReader(javafx.scene.Node node) {
        if (node instanceof javafx.scene.control.Labeled labeledNode) {
            labeledNode.setOnMouseEntered(event -> {
                if (com.energyapp.service.AccessibilityManager.screenReaderProperty().get()) {
                    String textToSpeak = labeledNode.getText();
                    if (textToSpeak != null && !textToSpeak.trim().isEmpty()) {
                        ttsService.speak(textToSpeak);
                    }
                }
            });
        }

        if (node instanceof javafx.scene.Parent parent) {
            for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
                attachScreenReader(child);
            }
        }

        if (node instanceof javafx.scene.control.ScrollPane sp && sp.getContent() != null) {
            attachScreenReader(sp.getContent());
        }
    }

    private void checkAndShowSetup() {
        Geocode homeProfileCode = homeProfileRepository.getHomeLocation("HOME_1");
        if (homeProfileCode == null) {
            Stage setupModalStage = new Stage();
            setupModalStage.initModality(Modality.APPLICATION_MODAL);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/energyapp/view/SetupModal.fxml"));
            Parent setupModalRoot;
            try {
                setupModalRoot = loader.load();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            setupModalStage.setTitle("Location Setup");
            Scene setupScene = new Scene(setupModalRoot, 400, 250);
            ThemeController.applyTheme(setupScene);
            setupModalStage.setScene(setupScene);

            setupModalStage.showAndWait();
        }
    }
}
