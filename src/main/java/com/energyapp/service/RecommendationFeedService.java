package com.energyapp.service;

import com.energyapp.model.EnergyRecord;
import com.energyapp.model.Recommendation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class RecommendationFeedService {

    public static final int MAX_VISIBLE_RECOMMENDATIONS = 5;

    private static final RecommendationFeedService INSTANCE = new RecommendationFeedService(new RecommendationEngine());

    private final RecommendationEngine recommendationEngine;
    private final List<Recommendation> feed = new ArrayList<>();
    private LocalDateTime lastProcessedTimestamp;
    private Set<String> previousActiveIds = new HashSet<>();

    RecommendationFeedService(RecommendationEngine recommendationEngine) {
        this.recommendationEngine = recommendationEngine;
    }

    public static RecommendationFeedService getInstance() {
        return INSTANCE;
    }

    public synchronized FeedUpdate updateRecommendations(EnergyRecord currentRecord) {
        if (currentRecord == null) {
            return new FeedUpdate(getVisibleRecommendations(), List.of());
        }

        if (Objects.equals(lastProcessedTimestamp, currentRecord.getTimestamp())) {
            return new FeedUpdate(getVisibleRecommendations(), List.of());
        }

        lastProcessedTimestamp = currentRecord.getTimestamp();

        List<Recommendation> activeRecommendations = recommendationEngine.getActiveRecommendations(currentRecord);
        Set<String> activeIds = activeRecommendations.stream()
                .map(Recommendation::getId)
                .collect(Collectors.toSet());

        List<String> insertedIds = new ArrayList<>();
        List<Recommendation> nextFeed = new ArrayList<>(feed);
        for (Recommendation activeRecommendation : activeRecommendations) {
            if (!previousActiveIds.contains(activeRecommendation.getId())) {
                if (!nextFeed.isEmpty() && nextFeed.get(nextFeed.size() - 1).getId().equals(activeRecommendation.getId())) {
                    Recommendation lastRecommendation = nextFeed.get(nextFeed.size() - 1);
                    nextFeed.set(nextFeed.size() - 1,
                            lastRecommendation.withShownAt(currentRecord.getTimestamp()).withActive(true));
                } else {
                    nextFeed.add(activeRecommendation.withShownAt(currentRecord.getTimestamp()).withActive(true));
                    insertedIds.add(activeRecommendation.getId());
                }
            }
        }

        for (int i = 0; i < nextFeed.size(); i++) {
            Recommendation recommendation = nextFeed.get(i);
            nextFeed.set(i, recommendation.withActive(false));
        }

        for (String activeId : activeIds) {
            for (int i = nextFeed.size() - 1; i >= 0; i--) {
                Recommendation recommendation = nextFeed.get(i);
                if (recommendation.getId().equals(activeId)) {
                    nextFeed.set(i, recommendation.withActive(true));
                    break;
                }
            }
        }

        while (nextFeed.size() > MAX_VISIBLE_RECOMMENDATIONS) {
            nextFeed.remove(0);
        }

        feed.clear();
        feed.addAll(nextFeed);
        previousActiveIds = new HashSet<>(activeIds);

        return new FeedUpdate(getVisibleRecommendations(), insertedIds);
    }

    public synchronized List<Recommendation> getVisibleRecommendations() {
        return new ArrayList<>(feed);
    }

    public synchronized void reset() {
        feed.clear();
        lastProcessedTimestamp = null;
        previousActiveIds.clear();
    }

    public static final class FeedUpdate {
        private final List<Recommendation> visibleRecommendations;
        private final Set<String> insertedIds;

        public FeedUpdate(List<Recommendation> visibleRecommendations, List<String> insertedIds) {
            this.visibleRecommendations = visibleRecommendations;
            this.insertedIds = new HashSet<>(insertedIds);
        }

        public List<Recommendation> getVisibleRecommendations() {
            return visibleRecommendations;
        }

        public boolean isNewlyInserted(String id) {
            return insertedIds.contains(id);
        }

        public int insertedCount() {
            return insertedIds.size();
        }

        public Set<String> getInsertedIds() {
            return Set.copyOf(insertedIds);
        }
    }
}
