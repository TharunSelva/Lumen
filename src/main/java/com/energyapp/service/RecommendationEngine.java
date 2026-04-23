package com.energyapp.service;

import com.energyapp.model.EnergyRecord;
import com.energyapp.model.Recommendation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RecommendationEngine {

    private final List<Recommendation> allRecommendations = new ArrayList<>();

    public RecommendationEngine() {
        // these recommendations are currently for testing. these are subject to change later, and more will be added.
        allRecommendations.add(new Recommendation(
                "high-solar",
                "Run heavy appliances now!",
                "Your solar panels are generating a lot of excess right now. It would be economical to charge your EV or run the washing, etc.",
                record -> record.getSolarGenKwh() > 1.75
        ));

        allRecommendations.add(new Recommendation(
           "high-price",
           "Delay non-essential energy use.",
           "Grid prices are currently high. Try to wait a few hours before running non-essential, heavy appliances.",
           record -> record.getPricePerKwh() > 25.0
        ));

        allRecommendations.add(new Recommendation(
                "high-carbon",
                "Carbon intensity on the grid is currently high (>150gCO2/kWh)!",
                "If you're concerned with keeping your carbon footprint low, consider waiting before running non-essential, heavy appliances",
                record -> record.getCarbon() > 150.0
        ));

        allRecommendations.add(new Recommendation(
                "low-battery",
                "Your household battery is running low.",
                "Be aware that your household battery is at a low percentage (<25%). Charge when solar is high or energy prices are low.",
                record -> record.getBatteryLevel() < 25
        ));

        allRecommendations.add(new Recommendation(
           "high-heating",
           "Heating is using a lot of energy.",
           "Be aware that your heating has exceeded 2kWh of energy consumption. Consider turning it down to save energy.",
           record -> record.getHeatingUsageKwh() > 2.0
        ));

    }

    public List<Recommendation> getActiveRecommendations(EnergyRecord currentRecord) {
        if (currentRecord == null) {
            return List.of();
        }

        LocalDateTime shownAt = currentRecord.getTimestamp() != null
                ? currentRecord.getTimestamp()
                : LocalDateTime.now();

        // stream basically works as a for loop (but cleaner). so, this method tests each recommendation against the predicates assigned to them.
        return allRecommendations.stream()
                .filter(recco -> recco.getCondition().test(currentRecord))
                .map(recco -> recco.withShownAt(shownAt).withActive(true))
                .collect(Collectors.toList());
    }

}
