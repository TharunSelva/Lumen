package com.energyapp.controller;

import com.energyapp.db.HomeProfileRepository;
import com.energyapp.model.Geocode;
import com.energyapp.db.OpenMeteoSolarService;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.control.Label;
import javafx.scene.control.Button;

import java.util.List;

public class ForecastController {

    @FXML private LineChart<?, ?> energyCostForecastLineChart;
    @FXML private Button forecastingReload;
    @FXML private Label lastUpdatedLabel;




}