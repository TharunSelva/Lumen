package com.energyapp.controller;

import com.energyapp.model.Device;
import com.energyapp.model.DevicePlanCard;
import com.energyapp.model.EnergyRecord;
import com.energyapp.model.ScheduledDevice;
import com.energyapp.model.WashingMachine;
import com.energyapp.service.DeviceService;
import com.energyapp.service.DevicesPlanService;
import com.energyapp.service.SimulationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DevicesController {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter WINDOW_FMT = DateTimeFormatter.ofPattern("EEE HH:mm");
    private static final String STATUS_ON = "ON";
    private static final String STATUS_OFF = "OFF";
    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String STATUS_SKIPPED = "SKIPPED";
    private static final String STATUS_SEPARATOR = " \u2022 ";

    @FXML private FlowPane deviceContainer;
    @FXML private VBox planCardsContainer;
    @FXML private Label devicesOnSummaryLabel;
    @FXML private Label scheduledTodaySummaryLabel;
    @FXML private Label nextRunSummaryLabel;
    @FXML private Label deviceCountLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label greenEnergyNowLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Label emptyStateLabel;
    @FXML private Button btnForAdd;

    private DeviceService deviceService;
    private DevicesPlanService planService;
    private SimulationService simulationService;
    private final Map<String, String> displayNameOverrides = new HashMap<>();
    private final Map<String, LocalDateTime> acceptedPlanSelections = new HashMap<>();

    @FXML
    public void initialize() {
        deviceContainer.setVgap(18.0);
        deviceContainer.setHgap(18.0);
        statusFilter.getItems().addAll("All", "On", "Off", "Scheduled");
        statusFilter.setValue("All");
        statusFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(String value) {
                return value == null ? "" : value;
            }

            @Override
            public String fromString(String string) {
                return string;
            }
        });
        searchField.textProperty().addListener((obs, oldValue, newValue) -> refreshDevicesPage());
        statusFilter.valueProperty().addListener((obs, oldValue, newValue) -> refreshDevicesPage());

        btnForAdd.setOnAction(event -> {
            try {
                openAddDeviceModal();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void setSimulationService(SimulationService service) {
        this.simulationService = service;
        this.deviceService = new DeviceService(service);
        this.planService = new DevicesPlanService();
        refreshDevicesPage();
        service.getCurrentEnergyRecord().addListener((observable, oldRec, newRec) -> Platform.runLater(this::refreshDevicesPage));
    }

    void setServicesForTest(DeviceService deviceService, DevicesPlanService planService, SimulationService simulationService) {
        this.deviceService = deviceService;
        this.planService = planService;
        this.simulationService = simulationService;
    }

    void refreshDevicesPage() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::refreshDevicesPage);
            return;
        }
        if (deviceService == null || planService == null) {
            return;
        }

        rebuildDisplayNameOverrides(deviceService.getAllDevices());
        renderSummaryStrip();
        renderLiveIndicators();
        renderPlanCards();
        renderDeviceCards();
    }

    private void renderSummaryStrip() {
        List<Device> devices = deviceService.getAllDevices();
        long devicesOn = devices.stream().filter(Device::isOn).count();
        long scheduledToday = devices.stream()
                .filter(device -> STATUS_SCHEDULED.equals(deviceService.presentationStatus(device)))
                .count();

        String nextRun = devices.stream()
                .filter(device -> device instanceof ScheduledDevice scheduledDevice && scheduledDevice.getSchedule() != null)
                .map(device -> nextRunTime((ScheduledDevice) device, currentReferenceTime()))
                .min(LocalDateTime::compareTo)
                .map(time -> time.format(WINDOW_FMT))
                .orElse("Nothing planned yet");

        devicesOnSummaryLabel.setText(devicesOn + " on right now");
        scheduledTodaySummaryLabel.setText(scheduledToday + " scheduled");
        nextRunSummaryLabel.setText(nextRun);
        deviceCountLabel.setText(devices.size() + " devices");
    }

    private void renderLiveIndicators() {
        EnergyRecord currentRecord = simulationService == null ? null : simulationService.getCurrentEnergyRecord().get();
        if (currentRecord == null) {
            currentPriceLabel.setText("--.- p/kWh");
            greenEnergyNowLabel.setText("Standard grid");
            greenEnergyNowLabel.getStyleClass().setAll("devices-live-indicator", "devices-live-indicator-neutral");
            return;
        }

        currentPriceLabel.setText(String.format("%.1f p/kWh", currentRecord.getPricePerKwh()));
        boolean greenerNow = currentRecord.getSolarGenKwh() > 0.0 || currentRecord.getCarbon() <= 150.0;
        greenEnergyNowLabel.setText(greenerNow ? "Greener now" : "Standard grid");
        greenEnergyNowLabel.getStyleClass().setAll(
                "devices-live-indicator",
                greenerNow ? "devices-live-indicator-good" : "devices-live-indicator-neutral"
        );
    }

    private void renderPlanCards() {
        planCardsContainer.getChildren().clear();

        List<DevicePlanCard> cards = planService.buildPlanCards(
                deviceService.getAllDevices(),
                currentReferenceTime(),
                acceptedPlanSelections
        );
        if (cards.isEmpty()) {
            Label emptyLabel = new Label("No plans are ready yet. Add a schedulable device or set a schedule to see recommendations.");
            emptyLabel.getStyleClass().add("devices-empty-copy");
            planCardsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (DevicePlanCard card : cards) {
            planCardsContainer.getChildren().add(createPlanCard(card));
        }
    }

    private void renderDeviceCards() {
        deviceContainer.getChildren().clear();

        List<Device> visibleDevices = deviceService.getAllDevices().stream()
                .filter(this::matchesSearch)
                .filter(this::matchesFilter)
                .sorted(Comparator
                        .comparing((Device device) -> !(device instanceof ScheduledDevice))
                        .thenComparing(this::displayNameFor, String.CASE_INSENSITIVE_ORDER))
                .toList();

        emptyStateLabel.setVisible(visibleDevices.isEmpty());
        emptyStateLabel.setManaged(visibleDevices.isEmpty());

        for (Device device : visibleDevices) {
            deviceContainer.getChildren().add(createDeviceCard(device));
        }
    }

    private VBox createPlanCard(DevicePlanCard card) {
        VBox root = new VBox(10);
        root.getStyleClass().add("devices-plan-card");

        Label eyebrow = new Label(card.deviceType().name().replace('_', ' '));
        eyebrow.getStyleClass().add("devices-card-eyebrow");

        Label title = new Label(displayNameFor(card.deviceId(), card.deviceName()));
        title.getStyleClass().add("devices-plan-title");

        Label purpose = new Label(card.purpose());
        purpose.getStyleClass().add("devices-plan-action");
        purpose.setWrapText(true);

        Label window = new Label(buildWindowText(card));
        window.getStyleClass().add("devices-card-meta");

        Label reason = new Label(card.reason());
        reason.getStyleClass().add("devices-card-copy");
        reason.setWrapText(true);

        Button usePlanButton = new Button(card.selectedPlan() ? "Scheduled" : "Use this card");
        usePlanButton.getStyleClass().addAll("devices-inline-button", "devices-inline-button-primary");
        if (card.selectedPlan()) {
            usePlanButton.getStyleClass().add("devices-inline-button-selected");
        }
        usePlanButton.setOnAction(event -> applyPlan(card.deviceId(), card.windowStart()));

        Button skipButton = new Button("Skip");
        skipButton.getStyleClass().add("devices-inline-button");
        skipButton.setOnAction(event -> applyPlanSkip(card.deviceId()));

        Button settingsButton = new Button("Open settings of device");
        settingsButton.getStyleClass().add("devices-inline-button");
        settingsButton.setOnAction(event -> {
            try {
                Device device = findDevice(card.deviceId());
                if (device != null) {
                    openDeviceSettings(device);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        HBox actions = new HBox(8, usePlanButton, skipButton, settingsButton);
        actions.getStyleClass().add("devices-card-actions");

        root.getChildren().addAll(eyebrow, title, purpose, window, reason, actions);
        return root;
    }

    private VBox createDeviceCard(Device device) {
        VBox root = new VBox(10);
        root.getStyleClass().add("devices-device-card");

        HBox header = new HBox();
        header.setSpacing(10);

        Label typeLabel = new Label(device.getDeviceType().name().replace('_', ' '));
        typeLabel.getStyleClass().add("devices-card-eyebrow");

        Label statusLabel = new Label(presentationStatus(device));
        statusLabel.getStyleClass().addAll("device-status-chip", statusClass(presentationStatus(device)));
        device.statusProperty().addListener((obs, oldValue, newValue) -> syncStatusLabel(statusLabel, device));
        device.isOnProperty().addListener((obs, oldValue, newValue) -> syncStatusLabel(statusLabel, device));
        if (device instanceof ScheduledDevice scheduledDevice) {
            scheduledDevice.scheduleProperty().addListener((obs, oldValue, newValue) -> syncStatusLabel(statusLabel, device));
            scheduledDevice.skipNextRunProperty().addListener((obs, oldValue, newValue) -> syncStatusLabel(statusLabel, device));
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(typeLabel, spacer, statusLabel);

        Label nameLabel = new Label(displayNameFor(device));
        nameLabel.getStyleClass().add("devices-device-title");
        nameLabel.setWrapText(true);

        Label stateLabel = new Label();
        stateLabel.getStyleClass().add("devices-card-copy");
        updateDeviceStateText(device, stateLabel);
        device.isOnProperty().addListener((obs, oldValue, newValue) -> updateDeviceStateText(device, stateLabel));
        device.statusProperty().addListener((obs, oldValue, newValue) -> updateDeviceStateText(device, stateLabel));
        if (device instanceof ScheduledDevice scheduledDevice) {
            scheduledDevice.scheduleProperty().addListener((obs, oldValue, newValue) -> updateDeviceStateText(device, stateLabel));
            scheduledDevice.skipNextRunProperty().addListener((obs, oldValue, newValue) -> updateDeviceStateText(device, stateLabel));
        }

        Label nextRunLabel = new Label(buildScheduleLabel(device));
        nextRunLabel.getStyleClass().add("devices-card-meta");
        if (device instanceof ScheduledDevice scheduledDevice) {
            scheduledDevice.scheduleProperty().addListener((obs, oldValue, newValue) -> nextRunLabel.setText(buildScheduleLabel(device)));
        }

        Button detailsButton = new Button("Open settings");
        detailsButton.getStyleClass().addAll("devices-inline-button", "devices-inline-button-primary");
        detailsButton.setOnAction(event -> {
            try {
                openDeviceSettings(device);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        root.getChildren().addAll(header, nameLabel, stateLabel, nextRunLabel, detailsButton);
        return root;
    }

    private void openAddDeviceModal() throws IOException {
        Stage addDeviceModalStage = new Stage();
        addDeviceModalStage.initModality(Modality.APPLICATION_MODAL);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/energyapp/view/AddDevice.fxml"));
        Parent addDeviceModalRoot = loader.load();

        addDeviceModalStage.setTitle("Add New Device");
        Scene addDeviceScene = new Scene(addDeviceModalRoot, 430, 390);
        ThemeController.applyTheme(addDeviceScene);
        addDeviceModalStage.setScene(addDeviceScene);
        addDeviceModalStage.sizeToScene();

        addDeviceModalStage.showAndWait();

        AddDeviceController controller = loader.getController();
        Device newDevice = controller.getNewlyCreatedDevice();
        if (newDevice != null) {
            deviceService.addDevice(newDevice);
            Platform.runLater(this::refreshDevicesPage);
        }
    }

    private void openDeviceSettings(Device device) throws IOException {
        Stage settingsModalStage = new Stage();
        settingsModalStage.initModality(Modality.APPLICATION_MODAL);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/energyapp/view/DeviceSettings.fxml"));
        Parent settingsModalRoot = loader.load();
        DeviceSettingsController controller = loader.getController();
        controller.init(deviceService, device, displayNameFor(device));

        settingsModalStage.setTitle("Device Settings");
        Scene settingsScene = new Scene(settingsModalRoot, 560, 520);
        ThemeController.applyTheme(settingsScene);
        settingsModalStage.setScene(settingsScene);

        settingsModalStage.showAndWait();
        if (!controller.isDeleted()) {
            deviceService.saveDevice(device);
        } else {
            acceptedPlanSelections.remove(device.getDeviceId());
        }
        Platform.runLater(this::refreshDevicesPage);
    }

    private void applyPlan(String deviceId, LocalDateTime windowStart) {
        Device device = findDevice(deviceId);
        if (device instanceof ScheduledDevice scheduledDevice) {
            deviceService.applyPlan(scheduledDevice, windowStart, currentReferenceTime());
            acceptedPlanSelections.put(deviceId, windowStart);
            refreshDevicesPage();
        }
    }

    private void applyPlanSkip(String deviceId) {
        Device device = findDevice(deviceId);
        if (device instanceof ScheduledDevice scheduledDevice) {
            deviceService.skipNextScheduledRun(scheduledDevice);
            acceptedPlanSelections.remove(deviceId);
            refreshDevicesPage();
        }
    }

    private Device findDevice(String deviceId) {
        return deviceService.getAllDevices().stream()
                .filter(device -> deviceId.equals(device.getDeviceId()))
                .findFirst()
                .orElse(null);
    }

    private boolean matchesSearch(Device device) {
        String searchText = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        if (searchText.isBlank()) {
            return true;
        }
        return displayNameFor(device).toLowerCase().contains(searchText)
                || device.getDeviceType().name().replace('_', ' ').toLowerCase().contains(searchText);
    }

    private boolean matchesFilter(Device device) {
        String selectedFilter = statusFilter.getValue();
        if (selectedFilter == null || selectedFilter.equals("All")) {
            return true;
        }

        return switch (selectedFilter) {
            case "On" -> STATUS_ON.equals(presentationStatus(device));
            case "Off" -> STATUS_OFF.equals(presentationStatus(device));
            case "Scheduled" -> STATUS_SCHEDULED.equals(presentationStatus(device));
            default -> true;
        };
    }

    private void updateDeviceStateText(Device device, Label stateLabel) {
        String state = device.isOn() ? "Power on" : "Power off";
        stateLabel.setText(state + STATUS_SEPARATOR + presentationStatus(device));
    }

    private String buildScheduleLabel(Device device) {
        if (device instanceof ScheduledDevice scheduledDevice && scheduledDevice.getSchedule() != null) {
            return "Next scheduled time: " + scheduledDevice.getSchedule().format(TIME_FMT);
        }
        return "No automatic schedule";
    }

    private LocalDateTime nextRunTime(ScheduledDevice device, LocalDateTime referenceTime) {
        LocalDateTime nextRun = referenceTime.toLocalDate().atTime(device.getSchedule());
        if (nextRun.isBefore(referenceTime)) {
            nextRun = nextRun.plusDays(1);
        }
        return nextRun;
    }

    private LocalDateTime currentReferenceTime() {
        if (simulationService != null && simulationService.getCurrentEnergyRecord().get() != null) {
            return simulationService.getCurrentEnergyRecord().get().getTimestamp();
        }
        return LocalDateTime.now();
    }

    private void rebuildDisplayNameOverrides(List<Device> devices) {
        displayNameOverrides.clear();
        List<Device> washingMachines = devices.stream()
                .filter(WashingMachine.class::isInstance)
                .sorted(Comparator.comparing(Device::getDeviceId, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        if (washingMachines.size() <= 1) {
            return;
        }

        for (int index = 0; index < washingMachines.size(); index++) {
            displayNameOverrides.put(washingMachines.get(index).getDeviceId(), "Washing Machine " + (index + 1));
        }
    }

    private String displayNameFor(Device device) {
        return displayNameOverrides.getOrDefault(device.getDeviceId(), fallbackName(device.getDeviceName(), device.getDeviceType().name()));
    }

    private String displayNameFor(String deviceId, String fallbackName) {
        return displayNameOverrides.getOrDefault(deviceId, fallbackName == null || fallbackName.isBlank()
                ? "Device"
                : fallbackName.trim());
    }

    private String fallbackName(String deviceName, String defaultName) {
        return deviceName == null || deviceName.isBlank()
                ? defaultName.replace('_', ' ')
                : deviceName.trim();
    }

    private String presentationStatus(Device device) {
        return deviceService.presentationStatus(device);
    }

    private String buildWindowText(DevicePlanCard card) {
        if (card.windowStart() == null) {
            return "No schedule yet";
        }
        String endText = card.windowEnd() == null ? "" : " to " + card.windowEnd().format(TIME_FMT);
        return card.windowStart().format(WINDOW_FMT) + endText;
    }

    private void syncStatusLabel(Label statusLabel, Device device) {
        String status = presentationStatus(device);
        statusLabel.setText(status);
        statusLabel.getStyleClass().setAll("device-status-chip", statusClass(status));
    }

    private String statusClass(String status) {
        return switch (status) {
            case STATUS_ON -> "device-status-on";
            case STATUS_SCHEDULED -> "device-status-scheduled";
            case STATUS_SKIPPED -> "device-status-skipped";
            default -> "device-status-off";
        };
    }
}
