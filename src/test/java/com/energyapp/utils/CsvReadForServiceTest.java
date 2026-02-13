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
}
