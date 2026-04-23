package com.energyapp.controller;

import com.energyapp.model.EnergyRecord;
import javafx.application.Platform;
import javafx.scene.chart.XYChart;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForecastingControllerTest {

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void buildPriceChartSeries_showsForecastUntilActualArrives() {
        ForecastingController controller = new ForecastingController();
        LocalDate chartDay = LocalDate.of(2026, 4, 21);

        List<EnergyRecord> history = List.of(
                record(chartDay.atTime(0, 30), 16.0)
        );

        List<EnergyRecord> forecast = List.of(
                record(chartDay.atTime(0, 0), 12.0),
                record(chartDay.atTime(0, 30), 13.0),
                record(chartDay.atTime(1, 0), 14.0)
        );

        List<XYChart.Series<String, Number>> series = controller.buildPriceChartSeries(history, forecast, chartDay);
        Map<String, List<XYChart.Data<String, Number>>> pointsBySeries = flattenSeries(series);

        assertEquals(List.of("00:30"), xValues(pointsBySeries.get("Actual Price")));
        assertEquals(List.of(16.0), yValues(pointsBySeries.get("Actual Price")));

        assertEquals(List.of("00:00", "01:00"), xValues(pointsBySeries.get("Forecast Price")));
        assertEquals(List.of(12.0, 14.0), yValues(pointsBySeries.get("Forecast Price")));
    }

    @Test
    void buildPriceChartSeries_usesForecastBaselineWhenNoActualExists() {
        ForecastingController controller = new ForecastingController();
        LocalDate chartDay = LocalDate.of(2026, 4, 21);

        List<EnergyRecord> forecast = List.of(
                record(chartDay.atTime(0, 0), 12.0),
                record(chartDay.atTime(0, 30), 13.0),
                record(chartDay.atTime(1, 0), 14.0)
        );

        List<XYChart.Series<String, Number>> series = controller.buildPriceChartSeries(List.of(), forecast, chartDay);
        Map<String, List<XYChart.Data<String, Number>>> pointsBySeries = flattenSeries(series);

        assertFalse(pointsBySeries.containsKey("Actual Price"));
        assertEquals(List.of("00:00", "00:30", "01:00"), xValues(pointsBySeries.get("Forecast Price")));
        assertEquals(List.of(12.0, 13.0, 14.0), yValues(pointsBySeries.get("Forecast Price")));
    }

    @Test
    void buildPriceChartSeries_onlyUsesActualPricesFromChartDay() {
        ForecastingController controller = new ForecastingController();
        LocalDate chartDay = LocalDate.of(2026, 4, 21);

        List<EnergyRecord> history = List.of(
                record(chartDay.minusDays(1).atTime(0, 0), 99.0),
                record(chartDay.atTime(0, 30), 18.0)
        );

        List<EnergyRecord> forecast = List.of(
                record(chartDay.atTime(0, 0), 12.0),
                record(chartDay.atTime(0, 30), 13.0)
        );

        List<XYChart.Series<String, Number>> series = controller.buildPriceChartSeries(history, forecast, chartDay);
        Map<String, List<XYChart.Data<String, Number>>> pointsBySeries = flattenSeries(series);

        assertEquals(List.of("00:30"), xValues(pointsBySeries.get("Actual Price")));
        assertEquals(List.of("00:00"), xValues(pointsBySeries.get("Forecast Price")));
        assertTrue(yValues(pointsBySeries.get("Forecast Price")).contains(12.0));
    }

    private static EnergyRecord record(LocalDateTime timestamp, double pricePerKwh) {
        return new EnergyRecord(timestamp, timestamp.toLocalTime().toString(), 0.0, pricePerKwh,
                0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0, 0.0);
    }

    private static Map<String, List<XYChart.Data<String, Number>>> flattenSeries(List<XYChart.Series<String, Number>> seriesList) {
        return seriesList.stream()
                .collect(Collectors.groupingBy(
                        XYChart.Series::getName,
                        Collectors.flatMapping(series -> series.getData().stream(), Collectors.toCollection(ArrayList::new))
                ));
    }

    private static List<String> xValues(List<XYChart.Data<String, Number>> points) {
        return points.stream().map(XYChart.Data::getXValue).toList();
    }

    private static List<Double> yValues(List<XYChart.Data<String, Number>> points) {
        return points.stream().map(point -> point.getYValue().doubleValue()).toList();
    }
}
