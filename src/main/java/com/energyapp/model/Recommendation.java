package com.energyapp.model;

import java.util.function.Predicate;

public class Recommendation {

    private String title;
    private String explanation;
    Predicate<EnergyRecord> condition;

    public Recommendation(String t, String e, Predicate<EnergyRecord> c) {
        this.title = t;
        this.explanation = e;
        this.condition = c;
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
}
