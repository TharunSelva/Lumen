package com.energyapp.service;

import com.energyapp.model.EnergyRecord;
import com.energyapp.model.Recommendation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RecommendationFeedServiceTest {

    private RecommendationFeedService feedService;

    @BeforeEach
    void setUp() {
        feedService = new RecommendationFeedService(new RecommendationEngine());
    }

    @Test
    void duplicateRecommendationsAreMergedAcrossTicks() {
        EnergyRecord first = new EnergyRecord(
                LocalDateTime.of(2026, 3, 31, 10, 0), "10:00", 0.0, 30.0, 50.0, 0.0,
                2.0, 50, 0.0, 0.0, 1.0, 0.0
        );
        EnergyRecord second = new EnergyRecord(
                LocalDateTime.of(2026, 3, 31, 10, 30), "10:30", 0.0, 31.0, 50.0, 0.0,
                2.2, 50, 0.0, 0.0, 1.0, 0.0
        );

        feedService.updateRecommendations(first);
        RecommendationFeedService.FeedUpdate update = feedService.updateRecommendations(second);

        long solarCount = update.getVisibleRecommendations().stream()
                .filter(rec -> rec.getId().equals("high-solar"))
                .count();

        assertEquals(1, solarCount);
    }

    @Test
    void feedCapsVisibleRecommendationsAtFive() {
        RecommendationFeedService service = new RecommendationFeedService(new RecommendationEngine() {
            @Override
            public List<Recommendation> getActiveRecommendations(EnergyRecord currentRecord) {
                return List.of(
                        new Recommendation("1", "A", "A", r -> true, currentRecord.getTimestamp(), true),
                        new Recommendation("2", "B", "B", r -> true, currentRecord.getTimestamp(), true),
                        new Recommendation("3", "C", "C", r -> true, currentRecord.getTimestamp(), true),
                        new Recommendation("4", "D", "D", r -> true, currentRecord.getTimestamp(), true),
                        new Recommendation("5", "E", "E", r -> true, currentRecord.getTimestamp(), true),
                        new Recommendation("6", "F", "F", r -> true, currentRecord.getTimestamp(), true)
                );
            }
        });

        EnergyRecord record = new EnergyRecord(
                LocalDateTime.of(2026, 3, 31, 11, 0), "11:00", 0.0, 0.0, 0.0, 0.0,
                0.0, 100, 0.0, 0.0, 0.0, 0.0
        );

        RecommendationFeedService.FeedUpdate update = service.updateRecommendations(record);
        assertEquals(RecommendationFeedService.MAX_VISIBLE_RECOMMENDATIONS, update.getVisibleRecommendations().size());
    }

    @Test
    void timestampIsPreservedOnExistingRecommendations() {
        EnergyRecord first = new EnergyRecord(
                LocalDateTime.of(2026, 3, 31, 8, 0), "08:00", 0.0, 30.0, 0.0, 0.0,
                2.0, 100, 0.0, 0.0, 0.0, 0.0
        );
        EnergyRecord second = new EnergyRecord(
                LocalDateTime.of(2026, 3, 31, 8, 30), "08:30", 0.0, 29.0, 0.0, 0.0,
                2.1, 100, 0.0, 0.0, 0.0, 0.0
        );

        feedService.updateRecommendations(first);
        Recommendation recommendation = feedService.updateRecommendations(second)
                .getVisibleRecommendations()
                .stream()
                .filter(rec -> rec.getId().equals("high-solar"))
                .findFirst()
                .orElseThrow();

        assertEquals(first.getTimestamp(), recommendation.getShownAt());
    }

    @Test
    void consecutiveDuplicateIsSuppressedButTimestampUpdates() {
        RecommendationFeedService service = new RecommendationFeedService(new RecommendationEngine() {
            @Override
            public List<Recommendation> getActiveRecommendations(EnergyRecord currentRecord) {
                if (currentRecord.getSolarGenKwh() > 1.75) {
                    return List.of(new Recommendation(
                            "high-solar",
                            "Run heavy appliances now!",
                            "Your solar panels are generating a lot of excess right now.",
                            record -> true,
                            currentRecord.getTimestamp(),
                            true
                    ));
                }
                return List.of();
            }
        });

        EnergyRecord first = new EnergyRecord(
                LocalDateTime.of(2026, 3, 31, 8, 0), "08:00", 0.0, 30.0, 0.0, 0.0,
                2.0, 100, 0.0, 0.0, 0.0, 0.0
        );
        EnergyRecord second = new EnergyRecord(
                LocalDateTime.of(2026, 3, 31, 8, 30), "08:30", 0.0, 15.0, 0.0, 0.0,
                0.0, 100, 0.0, 0.0, 0.0, 0.0
        );
        EnergyRecord third = new EnergyRecord(
                LocalDateTime.of(2026, 3, 31, 9, 0), "09:00", 0.0, 31.0, 0.0, 0.0,
                2.1, 100, 0.0, 0.0, 0.0, 0.0
        );

        service.updateRecommendations(first);
        service.updateRecommendations(second);
        RecommendationFeedService.FeedUpdate update = service.updateRecommendations(third);

        assertTrue(update.getInsertedIds().isEmpty());
        assertEquals(1, update.getVisibleRecommendations().size());
        assertEquals(third.getTimestamp(), update.getVisibleRecommendations().get(0).getShownAt());
    }
}
