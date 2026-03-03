package com.energyapp.service;

import com.energyapp.model.Insight;
import java.util.ArrayList;
import java.util.List;

public class InsightEngine {

    public List<Insight> generateInsights(double current, double previous) {
        List<Insight> results = new ArrayList<>();

        // Logic 1: Comparison Logic
        if (previous > 0) {
            double diff = previous - current;
            if (diff > 0) {
                double percent = (diff / previous) * 100;
                String msg = String.format("You saved %.2f kWh compared to last week. That's a %.1f%% decrease!", diff, percent);
                results.add(new Insight("Weekly Savings", msg, "leaf-icon", 1.0));
            } else if (diff < 0) {
                double percent = (Math.abs(diff) / previous) * 100;
                String msg = String.format("Alert: Usage increased by %.2f kWh compared to last week. That's a %.1f%% increase!", Math.abs(diff), percent);
                results.add(new Insight("Usage Alert", msg, "warning-icon", 0.9));
            }
        }

        // Logic 2: Contextual Advice (Simulation Templates)
        results.add(getRandomAdvice());

        return results;
    }

    private Insight getRandomAdvice() {
        String[] titles = {"Solar Tip", "Battery Tip", "Grid Saving"};
        String[] tips = {
                "High solar production expected at 12:00. Schedule your wash then!",
                "Your battery is currently discharging to save you peak-time costs.",
                "Energy prices are low tonight. Best time to charge the EV."
        };
        int index = (int) (Math.random() * tips.length);
        return new Insight(titles[index], tips[index], "bulb-icon", 0.5);
    }
}