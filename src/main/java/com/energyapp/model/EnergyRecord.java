package com.energyapp.model;

import java.time.LocalDateTime;

public class EnergyRecord {

    private LocalDateTime timestamp;
    private double gridImport;
    private double pricePerKwh;

    public EnergyRecord(LocalDateTime timestamp, double gridImport, double pricePerKwh) {
        this.timestamp = timestamp;
        this.gridImport = gridImport;
        this.pricePerKwh = pricePerKwh;
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

    public double calculateEnergyCost() {
        return this.gridImport * this.pricePerKwh;
    }

}
