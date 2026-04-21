package com.energyapp.utils;
import com.energyapp.model.EnergyRecord;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CsvReadForServiceTest {

    @Test
    void testLoadData_ParsesCorrectly() {
        String csvData = "Date,Label,Price,Carbon,Load,Solar,Bat,Grid,EV,Wash,Heat,Base\n" +
                "2024-01-01 12:00:00,L,0.50,100,10.0,5.0,50,2.0,1.0,1.0,1.0,1.0";

        InputStream stream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));

        List<EnergyRecord> results = CsvReadForService.loadData(stream);

        assertEquals(1, results.size());

        EnergyRecord rec = results.get(0);
        assertEquals(0.50, rec.getPricePerKwh(), 0.001);
        assertEquals(10.0, rec.getTotalLoadKwh(), 0.001);
        assertEquals(2.0, rec.getGridImport(), 0.001);
    }

    @Test
    void testLoadData_HandlesEmptyStream() {
        String csvData = "Date,Label,Price...\n";
        InputStream stream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<EnergyRecord> results = CsvReadForService.loadData(stream);
        assertTrue(results.isEmpty());
    }

    @Test
    void testLoadData_InvalidFormat_StopsReading() {
        String badCsv = "Date,Label,Price,Carbon,Load,Solar,Bat,Grid,EV,Wash,Heat,Base\n" +
                "2024-01-01 12:00:00,L,0.50,100,10.0,5.0,50,2.0,1.0,1.0,1.0,1.0\n" +
                "2024-01-01 12:30:00,L,Invalid,100,10.0,5.0,50,2.0,1.0,1.0,1.0,1.0\n" + // <--- Bad Line
                "2024-01-01 13:00:00,L,0.50,100,10.0,5.0,50,2.0,1.0,1.0,1.0,1.0";

        InputStream stream = new ByteArrayInputStream(badCsv.getBytes(StandardCharsets.UTF_8));
        List<EnergyRecord> results = CsvReadForService.loadData(stream);

        assertEquals(1, results.size(), "Should have read the first valid record before crashing on the second");
    }

    @Test
    void testLoadData_MultipleValidRecords() {
        String csvData = "Date,Label,Price,Carbon,Load,Solar,Bat,Grid,EV,Wash,Heat,Base\n" +
                "2024-01-01 12:00:00,12:00,20.0,100,10.0,5.0,50,2.0,1.0,1.0,1.0,1.0\n" +
                "2024-01-01 12:30:00,12:30,25.0,120,12.0,6.0,55,3.0,1.5,1.2,0.8,1.5\n" +
                "2024-01-01 13:00:00,13:00,15.0,80,8.0,4.0,60,1.0,0.5,0.8,1.2,0.8";

        InputStream stream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<EnergyRecord> results = CsvReadForService.loadData(stream);

        assertEquals(3, results.size());
        assertEquals(20.0, results.get(0).getPricePerKwh(), 0.001);
        assertEquals(25.0, results.get(1).getPricePerKwh(), 0.001);
        assertEquals(15.0, results.get(2).getPricePerKwh(), 0.001);
    }

    @Test
    void testLoadData_ParsesAllFields() {
        String csvData = "Date,Label,Price,Carbon,Load,Solar,Bat,Grid,EV,Wash,Heat,Base\n" +
                "2024-06-15 14:30:00,14:30,25.5,150,15.0,8.5,75,4.5,2.5,1.5,2.0,0.75";

        InputStream stream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<EnergyRecord> results = CsvReadForService.loadData(stream);

        assertEquals(1, results.size());
        EnergyRecord rec = results.get(0);

        assertEquals(25.5, rec.getPricePerKwh(), 0.001);
        assertEquals(150.0, rec.getCarbon(), 0.001);
        assertEquals(15.0, rec.getTotalLoadKwh(), 0.001);
        assertEquals(8.5, rec.getSolarGenKwh(), 0.001);
        assertEquals(75, rec.getBatteryLevel());
        assertEquals(4.5, rec.getGridImport(), 0.001);
        assertEquals(2.5, rec.getEvChargerUsageKwh(), 0.001);
        assertEquals(1.5, rec.getWashingMachineUsageKwh(), 0.001);
        assertEquals(2.0, rec.getHeatingUsageKwh(), 0.001);
        assertEquals(0.75, rec.getBaseloadKwh(), 0.001);
    }

    @Test
    void testLoadData_ZeroValues() {
        String csvData = "Date,Label,Price,Carbon,Load,Solar,Bat,Grid,EV,Wash,Heat,Base\n" +
                "2024-01-01 00:00:00,00:00,0.0,0,0.0,0.0,0,0.0,0.0,0.0,0.0,0.0";

        InputStream stream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<EnergyRecord> results = CsvReadForService.loadData(stream);

        assertEquals(1, results.size());
        EnergyRecord rec = results.get(0);

        assertEquals(0.0, rec.getPricePerKwh(), 0.001);
        assertEquals(0.0, rec.getCarbon(), 0.001);
        assertEquals(0.0, rec.getTotalLoadKwh(), 0.001);
        assertEquals(0.0, rec.getSolarGenKwh(), 0.001);
        assertEquals(0, rec.getBatteryLevel());
        assertEquals(0.0, rec.getGridImport(), 0.001);
    }

    @Test
    void testLoadData_HighValues() {
        String csvData = "Date,Label,Price,Carbon,Load,Solar,Bat,Grid,EV,Wash,Heat,Base\n" +
                "2024-01-01 12:00:00,12:00,100.0,500,50.0,25.0,100,30.0,10.0,5.0,8.0,3.0";

        InputStream stream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<EnergyRecord> results = CsvReadForService.loadData(stream);

        assertEquals(1, results.size());
        EnergyRecord rec = results.get(0);

        assertEquals(100.0, rec.getPricePerKwh(), 0.001);
        assertEquals(500.0, rec.getCarbon(), 0.001);
        assertEquals(50.0, rec.getTotalLoadKwh(), 0.001);
        assertEquals(100, rec.getBatteryLevel());
    }

    @Test
    void testLoadData_DecimalPrecision() {
        String csvData = "Date,Label,Price,Carbon,Load,Solar,Bat,Grid,EV,Wash,Heat,Base\n" +
                "2024-01-01 12:00:00,12:00,12.345,67.89,1.234,5.678,50,9.012,3.456,7.89,0.123,4.567";

        InputStream stream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<EnergyRecord> results = CsvReadForService.loadData(stream);

        assertEquals(1, results.size());
        EnergyRecord rec = results.get(0);

        assertEquals(12.345, rec.getPricePerKwh(), 0.001);
        assertEquals(67.89, rec.getCarbon(), 0.001);
        assertEquals(1.234, rec.getTotalLoadKwh(), 0.001);
        assertEquals(5.678, rec.getSolarGenKwh(), 0.001);
    }

    @Test
    void testLoadData_OnlyHeaderNoData() {
        String csvData = "Date,Label,Price,Carbon,Load,Solar,Bat,Grid,EV,Wash,Heat,Base\n";

        InputStream stream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<EnergyRecord> results = CsvReadForService.loadData(stream);

        assertTrue(results.isEmpty());
    }

    @Test
    void testLoadData_NullStream() {
        List<EnergyRecord> results = CsvReadForService.loadData(null);
        assertTrue(results.isEmpty());
    }

    @Test
    void testLoadData_LargeDataset() {
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("Date,Label,Price,Carbon,Load,Solar,Bat,Grid,EV,Wash,Heat,Base\n");
        for (int i = 0; i < 100; i++) {
            csvBuilder.append(String.format("2024-01-01 %02d:00:00,%02d:00,%d,100,10.0,5.0,50,2.0,1.0,1.0,1.0,1.0%n", i % 24, i % 24, i));
        }

        InputStream stream = new ByteArrayInputStream(csvBuilder.toString().getBytes(StandardCharsets.UTF_8));
        List<EnergyRecord> results = CsvReadForService.loadData(stream);

        assertEquals(100, results.size());
    }
}
