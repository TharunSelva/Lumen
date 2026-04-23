package com.energyapp.model;

import java.time.LocalDateTime;
import java.util.function.Predicate;

public class Recommendation {

    private final String id;
    private final String title;
    private final String explanation;
    private final Predicate<EnergyRecord> condition;
    private final LocalDateTime shownAt;
    private final boolean active;

    public Recommendation(String id, String t, String e, Predicate<EnergyRecord> c) {
        this(id, t, e, c, null, false);
    }

    public Recommendation(String id, String t, String e, Predicate<EnergyRecord> c, LocalDateTime shownAt, boolean active) {
        this.id = id;
        this.title = t;
        this.explanation = e;
        this.condition = c;
        this.shownAt = shownAt;
        this.active = active;
    }

    public String getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public String getExplanation() {
        return this.explanation;
    }

    public Predicate<EnergyRecord> getCondition() {
        return this.condition;
    }

    public LocalDateTime getShownAt() {
        return shownAt;
    }

    public boolean isActive() {
        return active;
    }

    public Recommendation withShownAt(LocalDateTime timestamp) {
        return new Recommendation(id, title, explanation, condition, timestamp, active);
    }

    public Recommendation withActive(boolean active) {
        return new Recommendation(id, title, explanation, condition, shownAt, active);
    }
}
