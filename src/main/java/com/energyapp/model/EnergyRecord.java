package com.energyapp.model;

import java.time.LocalDateTime;

public class EnergyRecord {

    private LocalDateTime timestamp;
    private double gridImport;
    private double pricePerKwh;
    private double carbon;
    private double totalLoadKwh;
    private double solarGenKwh;
    private double watts;
    private int batteryLevel;
    private double evChargerUsageKwh;
    private double washingMachineUsageKwh;
    private double heatingUsageKwh;
    private double baseloadKwh;
    private String timeLabel;

    public EnergyRecord(LocalDateTime timestamp, String timeLabel, double gridImport, double pricePerKwh, double carbon, double totalLoadKwh,
                        double solarGenKwh, int batteryLevel, double evChargerUsageKwh, double washingMachineUsageKwh,
                        double heatingUsageKwh, double baseloadKwh) {
        this.timestamp = timestamp;
        this.timeLabel = timeLabel;
        this.gridImport = gridImport;
        this.pricePerKwh = pricePerKwh;
        this.carbon = carbon;
        this.totalLoadKwh = totalLoadKwh;
        this.solarGenKwh = solarGenKwh;
        this.watts = totalLoadKwh * 2000.0;
        this.batteryLevel = batteryLevel;
        this.evChargerUsageKwh = evChargerUsageKwh;
        this.washingMachineUsageKwh = washingMachineUsageKwh;
        this.heatingUsageKwh = heatingUsageKwh;
        this.baseloadKwh = baseloadKwh;
    }

    public LocalDateTime getTimestamp() {
        return this.timestamp;
    }
    public double getGridImport() {
        return this.gridImport;
    }
    public double getPricePerKwh() {
        return this.pricePerKwh;
    }
    public double getCarbon() {
        return this.carbon;
    }
    public double getTotalLoadKwh() {
        return this.totalLoadKwh;
    }
    public double getSolarGenKwh() {
        return this.solarGenKwh;
    }
    public double getWatts() {
        return this.watts;
    }
    public int getBatteryLevel() {
        return this.batteryLevel;
    }
    public double getEvChargerUsageKwh() {
        return this.evChargerUsageKwh;
    }
    public double getWashingMachineUsageKwh() {
        return this.washingMachineUsageKwh;
    }
    public double getHeatingUsageKwh() {
        return this.heatingUsageKwh;
    }
    public double getBaseloadKwh() {
        return this.baseloadKwh;
    }

    public double calculateEnergyCost() {
        return this.gridImport * this.pricePerKwh;
    }

}
