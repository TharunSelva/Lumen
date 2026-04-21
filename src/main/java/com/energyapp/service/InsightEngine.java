package com.energyapp.service;

import com.energyapp.db.QuickTipRepository;
import com.energyapp.model.Insight;
import com.energyapp.model.QuickTip;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class InsightEngine {

    private final QuickTipRepository tipRepository;
    private final Random random;

    public InsightEngine() {
        this.tipRepository = new QuickTipRepository();
        this.random = new Random();
    }

    // Constructor for testing (allows dependency injection)
    InsightEngine(QuickTipRepository repository, Random random) {
        this.tipRepository = repository;
        this.random = random;
    }

    public List<Insight> generateInsights(double current, double previous) {
        List<Insight> results = new ArrayList<>();

        // Logic 1: Comparison Logic
        if (previous > 0) {
            double diff = previous - current;
            if (diff > 0) {
                double percent = (diff / previous) * 100;
                String msg = String.format("You saved %.2f kWh compared to the previous interval. That's a %.1f%% decrease!", diff, percent);
                results.add(new Insight("Weekly Savings", msg, "leaf-icon", 1.0));
            } else if (diff < 0) {
                double percent = (Math.abs(diff) / previous) * 100;
                String msg = String.format("Alert: Usage increased by %.2f kWh compared to previous interval. That's a %.1f%% increase!", Math.abs(diff), percent);
                results.add(new Insight("Usage Alert", msg, "warning-icon", 0.9));
            }
        }

        // Logic 2: Contextual Advice – now from database
        results.add(getRandomAdviceFromDb());

        return results;
    }

    private Insight getRandomAdviceFromDb() {
        List<QuickTip> allTips = tipRepository.fetchAllTips();
        if (allTips.isEmpty()) {
            return new Insight("Energy Tip", "Check your appliance usage to save energy.", "bulb-icon", 0.5);
        }
        QuickTip tip = allTips.get(random.nextInt(allTips.size()));
        return new Insight(tip.getTitle(), tip.getBody(), "bulb-icon", 0.5);
    }
}