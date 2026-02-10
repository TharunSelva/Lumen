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

    public EnergyRecord(LocalDateTime timestamp, double gridImport, double pricePerKwh, double carbon, double totalLoadKwh,
                        double solarGenKwh) {
        this.timestamp = timestamp;
        this.gridImport = gridImport;
        this.pricePerKwh = pricePerKwh;
        this.carbon = carbon;
        this.totalLoadKwh = totalLoadKwh;
        this.solarGenKwh = solarGenKwh;
        this.watts = totalLoadKwh * 2000.0;
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

    public double calculateEnergyCost() {
        return this.gridImport * this.pricePerKwh;
    }

}
