package com.energyapp.utils;

import com.energyapp.model.EnergyRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for reading CSV data from simulation_data.csv.
 * One-off usage inside SimulationService.java
 */
public final class CsvReadForService {

    // sim data filepath and date format parser for the csv file
    private static String filepath = "/com/simulation_data.csv";
    private static DateTimeFormatter csvDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");;

    /**
     * Method for reading data from the CSV file simulation_data.csv
     * One-off usage in App.java to instantiate loading off data into SimulationService.
     * @return simulationData to be used in SimulationService.
     */
    public static List<EnergyRecord> loadData() {
        List<EnergyRecord> simulationData = new ArrayList<>();
        try (BufferedReader br = new BufferedReader((new InputStreamReader(CsvReadForService.class.getResourceAsStream(filepath))))) {
            br.readLine();

            String line;
            // read all records, make them into EnergyRecords, and add to simulationData array.
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                LocalDateTime timestamp = LocalDateTime.parse(values[0], csvDateFormatter);
                String timeLabel = values[1];
                double pricePerKwh = Double.parseDouble(values[2]);
                double carbon = Double.parseDouble(values[3]);
                double totalLoadKwh = Double.parseDouble(values[4]);
                double solarGenKwh = Double.parseDouble(values[5]);
                int batteryLevel = Integer.parseInt(values[6]);
                double gridImport = Double.parseDouble(values[7]);
                double evChargerUsageKwh = Double.parseDouble(values[8]);
                double washingMachineUsageKwh = Double.parseDouble(values[9]);
                double heatingUsageKwh = Double.parseDouble(values[10]);
                double baseloadKwh = Double.parseDouble(values[11]);

                EnergyRecord energyRecord = new EnergyRecord(timestamp, timeLabel, gridImport, pricePerKwh, carbon, totalLoadKwh,
                        solarGenKwh, batteryLevel, evChargerUsageKwh, washingMachineUsageKwh, heatingUsageKwh, baseloadKwh);
                simulationData.add(energyRecord);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // return simulationData to be used in the SimulationService.
        return simulationData;
    }

}
