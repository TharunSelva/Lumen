package com.energyapp.db;

import com.energyapp.model.Geocode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches hourly shortwave radiation (W/m²) from Open-Meteo for a given location.
 * We store this into forecast_interval_cache.solar_shortwave_wm2.
 */
public class OpenMeteoSolarService {

    private final HttpClient httpClient;

    public OpenMeteoSolarService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static final class SolarPoint {
        public final String timeIso;         // e.g. "2026-03-05T01:00"
        public final double shortwaveWm2;     // W/m²

        public SolarPoint(String timeIso, double shortwaveWm2) {
            this.timeIso = timeIso;
            this.shortwaveWm2 = shortwaveWm2;
        }
    }

    /**
     * Returns hourly solar radiation points for the next N forecast days (typically 2).
     */
    public List<SolarPoint> fetchHourlyShortwave(double lat, double lon, int forecastDays)
            throws IOException, InterruptedException {

        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + lat
                + "&longitude=" + lon
                + "&hourly=shortwave_radiation"
                + "&timezone=UTC"
                + "&forecast_days=" + forecastDays;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("Open-Meteo HTTP " + resp.statusCode() + ": " + resp.body());
        }

        return parse(resp.body());
    }

    private List<SolarPoint> parse(String json) {
        List<SolarPoint> out = new ArrayList<>();

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject hourly = root.getAsJsonObject("hourly");
        if (hourly == null) return out;

        JsonArray times = hourly.getAsJsonArray("time");
        JsonArray vals = hourly.getAsJsonArray("shortwave_radiation");
        if (times == null || vals == null) return out;

        int n = Math.min(times.size(), vals.size());
        for (int i = 0; i < n; i++) {
            String t = times.get(i).getAsString();     // "YYYY-MM-DDTHH:MM"
            double v = vals.get(i).isJsonNull() ? 0.0 : vals.get(i).getAsDouble();
            out.add(new SolarPoint(t, v));
        }

        return out;
    }

    public Geocode geocoder(String cityName) throws IOException, IllegalArgumentException, InterruptedException {
        String encodedCity = URLEncoder.encode(cityName, StandardCharsets.UTF_8);
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encodedCity + "&count=1&format=json";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Open-Meteo Geocoding failed with exit code: " + response.statusCode() + ": " + response.body());
        }

        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray resultsArray = jsonResponse.getAsJsonArray("results");

        double lat = 0;
        double lon = 0;
        if (resultsArray != null && !resultsArray.isEmpty()) {
            JsonObject firstResult = resultsArray.get(0).getAsJsonObject();
            lat = firstResult.get("latitude").getAsDouble();
            lon = firstResult.get("longitude").getAsDouble();
        } else {
            throw new IllegalArgumentException("City not found");
        }

        return new Geocode(
                lat,
                lon
        );

    }
}














