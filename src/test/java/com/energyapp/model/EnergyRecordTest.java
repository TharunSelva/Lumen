package com.energyapp.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class EnergyRecordTest {

    @Test
    void testConstructor_CalculatesDerivedFields() {
        LocalDateTime now = LocalDateTime.now();
        double loadKwh = 1.5;

        EnergyRecord record = new EnergyRecord(
                now, "Label", 0.0, 0.0, 0.0,
                loadKwh,
                0.0, 0, 0.0, 0.0, 0.0, 0.0
        );

        assertEquals(3000.0, record.getWatts(), 0.001, "Watts should be derived from Load Kwh * 2000");
    }
}