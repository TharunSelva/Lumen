package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import com.energyapp.service.SimulationService;

import com.energyapp.db.HomeProfileRepository;
import com.energyapp.model.Geocode;
import com.energyapp.db.OpenMeteoSolarService;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Button;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.List;

public class ForecastController {

    @FXML private LineChart<String, Number> energyCostForecastLineChart;
    @FXML private Button forecastingReload;
    @FXML private Label lastUpdatedLabel;

    private XYChart.Series<String, Number> energyCostSeries = new XYChart.Series<>();

    private List<EnergyRecord> energyRecords = new ArrayList<>();
    private List<EnergyRecord> forecastEnergyRecords;

    @FXML
    public void initialize() {
        energyCostSeries.setName("Energy Cost");
        energyCostForecastLineChart.getData().add(energyCostSeries);
    }

    public void setSimulationService (SimulationService simulationService) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        simulationService.getCurrentEnergyRecord().addListener((obs, oldVal, newVal) -> {
            updateGraph(newVal);
            lastUpdatedLabel.setText(LocalTime.now().format(formatter));
        });
    }

    private void updateGraph(EnergyRecord newVal) {
        energyRecords.add(newVal);
        forecastEnergyRecords = predict(energyRecords);
        energyCostSeries.getData().clear();
        energyCostSeries.getData().addAll(plotData().getData());
    }

    private List<EnergyRecord> predict(List<EnergyRecord> records) {
        List<EnergyRecord> result = new ArrayList<>(Collections.nCopies(48, null));
        for (EnergyRecord record : records) {
            int index = getTimeSlotIndex(record.getTimestamp());
            if (index >= 0 && index < 48) {
                result.set(index, record);
            }
        }
        return result;
    }

    private int getTimeSlotIndex(LocalDateTime dateTime) {
        int hour = dateTime.getHour();
        int minute = dateTime.getMinute();
        return (int) Math.round(2 * (hour + minute / 60.0));
    }

    private String formatTimeSlot(int slotIndex) {
        int hour = slotIndex / 2;
        int minute = (slotIndex % 2) * 30;
        return String.format("%02d:%02d", hour, minute);
    }

    private XYChart.Series<String, Number> plotData() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (int i = 0; i < forecastEnergyRecords.size(); i++) {
            EnergyRecord energyRecord = forecastEnergyRecords.get(i);
            if (energyRecord != null) {
                String x = formatTimeSlot(i);
                double y = energyRecord.getPricePerKwh();
                series.getData().add(new XYChart.Data<>(x, y));
            }
        }
        return series;
    }
}