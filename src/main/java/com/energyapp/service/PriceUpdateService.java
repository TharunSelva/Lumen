package com.energyapp.service;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;

public class PriceUpdateService extends ScheduledService<Double> {
    public PriceUpdateService() {
        setPeriod(Duration.seconds(5));
        setDelay(Duration.seconds(1));
    }

    @Override
    protected Task<Double> createTask() {
        return new Task<>() {
            @Override
            protected Double call() throws Exception {
                return Math.random() * 0.5;
            }
        };
    }
}