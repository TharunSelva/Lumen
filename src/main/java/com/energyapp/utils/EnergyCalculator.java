package com.energyapp.utils;

import com.energyapp.model.EnergyRecord;

import java.util.ArrayList;

/**
 * Static class for calculating the cost of certain energy usages.
 */
public final class EnergyCalculator {

    /**
     * Calculates cost in £ for a given energy usage.
     *
     * @param kwh Energy usage (grid import, or total load)
     * @param pricePerKwh Price in pence
     * @return Cost in £
     */
    public static double calculateCostInPounds(double kwh, double pricePerKwh) {
        if (kwh < 0 || pricePerKwh < 0) {
            return 0.0;
        }
        return (kwh * pricePerKwh) / 100.0;
    }

    /**
     *
     * @param cost Cost in pence
     * @return Usage level
     */
    public static String determineEnergyUsageLevel(double cost) {
        if (cost < 0.05) return "Low Usage";
        if (cost < 0.15) return "Standard Usage";
        return "High Usage";
    }

    public static String determineSolarGenLevel(EnergyRecord rec, double genPct) {
        genPct *= 100;
        if (genPct > 35) {
            return "Status: Optimal";
        } else if (genPct == 0) {
            return "Status: Not Generating";
        } else {
            return "Status: Underperforming";
        }
    }


    public static double calculateSolarUsed(double totalLoad, double solarGen) {
        return Math.min(solarGen, totalLoad);
    }

    public static double calculateBatteryUsed(double totalLoad, double solarGen, double gridImport) {
        double solarUsed = calculateSolarUsed(totalLoad, solarGen);
        double remaining = Math.max(totalLoad - solarUsed, 0);
        double gridUsed = Math.min(gridImport, remaining);
        return Math.max(totalLoad - solarUsed - gridUsed, 0);
    }

    public static int calculatePercentage(double part, double total) {
        if (total <= 0.000001) return 0;
        return (int) Math.round((part / total) * 100);
    }

    public static String determineCo2Label(double carbon) {
        if (carbon <= 150) return "Low CO2";
        if (carbon <= 250) return "Medium CO2";
        return "High CO2";
    }

    public static ArrayList<Double> solarStatusCalc(EnergyRecord rec, double maxSolarCapacity) {
        double currentGen = rec.getSolarGenKwh();
        double progressBarValue = currentGen / maxSolarCapacity;
        ArrayList<Double> returnList = new ArrayList<>();
        returnList.add(currentGen);
        returnList.add(progressBarValue);

        return returnList;
    }


/**
 * Calculates the percentage of energy coming from clean/solar sources.
 *
 * @param totalLoad Total energy being used (Solar + Grid)
 * @param solar     Energy provided by solar
 * @return Percentage of clean energy (0.0 to 100.0)
 */
public static double calculateCleanShare(double totalLoad, double solar) {
    if (totalLoad <= 0) {
        return 0.0;
    }
    return (solar / totalLoad) * 100.0;

}
}
