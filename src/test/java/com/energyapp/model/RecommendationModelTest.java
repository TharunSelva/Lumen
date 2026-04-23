package com.energyapp.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

public class RecommendationModelTest {

    @Test
    void testRecommendation_Immutability() {
        Predicate<EnergyRecord> mockCondition = record -> true;
        Recommendation original = new Recommendation("REC_01", "Turn off TV", "Standby wastes energy", mockCondition);

        assertFalse(original.isActive());
        assertNull(original.getShownAt());

        // Transform state
        LocalDateTime now = LocalDateTime.now();
        Recommendation updated1 = original.withShownAt(now);
        Recommendation updated2 = updated1.withActive(true);

        // Verify Original remains unchanged
        assertNull(original.getShownAt());
        assertFalse(original.isActive());

        // Verify Updated objects have correct state
        assertEquals(now, updated1.getShownAt());
        assertFalse(updated1.isActive());

        assertEquals(now, updated2.getShownAt());
        assertTrue(updated2.isActive());
    }

    @Test
    void testRecommendation_EvaluatesCondition() {
        // Create a condition: Trigger if price > 20p
        Predicate<EnergyRecord> highPriceCondition = record -> {
            try {
                java.lang.reflect.Field priceField = EnergyRecord.class.getDeclaredField("pricePerKwh");
                priceField.setAccessible(true);
                return priceField.getDouble(record) > 20.0;
            } catch (Exception e) {
                return false;
            }
        };

        Recommendation rec = new Recommendation("REC_02", "Delay washing", "Price is high", highPriceCondition);

        // Inject dummy data
        EnergyRecord cheapRecord = createRecordWithPrice(10.0);
        EnergyRecord expensiveRecord = createRecordWithPrice(35.0);

        assertFalse(rec.getCondition().test(cheapRecord), "Condition should fail for cheap record");
        assertTrue(rec.getCondition().test(expensiveRecord), "Condition should trigger for expensive record");
    }

    private EnergyRecord createRecordWithPrice(double price) {
        // Standard constructor signature for EnergyRecord
        return new EnergyRecord(
                LocalDateTime.now(), "12:00", 0.0, price, 0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0, 0.0
        );
    }
}