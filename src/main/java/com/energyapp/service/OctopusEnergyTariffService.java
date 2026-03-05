package com.energyapp.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


// Service for fetching electricity tariff rates from the Octopus Energy API.
public class OctopusEnergyTariffService {

    private static final String BASE_URL =
            "https://api.octopus.energy/v1/products/AGILE-24-10-01/electricity-tariffs/E-1R-AGILE-24-10-01-A/standard-unit-rates/";

    // The API returns half-hourly records (100 results = 50 hours.)
    private static final int MAX_RESULTS_PER_REQUEST = 100;
    private static final int MINUTES_PER_SLOT = 30;
    private static final long MAX_SLOTS_PER_REQUEST = MAX_RESULTS_PER_REQUEST;

    private final HttpClient httpClient;

    public OctopusEnergyTariffService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    OctopusEnergyTariffService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }


    // Fetches Agile electricity tariff rates between the given dates.
    public List<Map.Entry<LocalDateTime, Double>> getUnitRates(LocalDateTime from, LocalDateTime to)
            throws IOException, InterruptedException {

        List<Map.Entry<LocalDateTime, Double>> allResults = new ArrayList<>();

        LocalDateTime batchStart = from;

        while (batchStart.isBefore(to)) {
            // Calculate the end of this batch (at most MAX_SLOTS_PER_REQUEST half-hours)
            LocalDateTime batchEnd = batchStart.plus(MAX_SLOTS_PER_REQUEST * MINUTES_PER_SLOT, ChronoUnit.MINUTES);
            if (batchEnd.isAfter(to)) {
                batchEnd = to;
            }

            List<Map.Entry<LocalDateTime, Double>> batch = fetchBatch(batchStart, batchEnd);
            allResults.addAll(batch);

            batchStart = batchEnd;
        }

        return allResults;
    }


    // Fetches a single batch of up to 100 results from the API.
    private List<Map.Entry<LocalDateTime, Double>> fetchBatch(LocalDateTime from, LocalDateTime to)
            throws IOException, InterruptedException {

        String url = BASE_URL
                + "?period_from=" + formatDateTime(from)
                + "&period_to=" + formatDateTime(to);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Octopus API returned HTTP " + response.statusCode()
                    + ": " + response.body());
        }

        return parseResults(response.body());
    }


    // JSON Parser, can potetnitally be replaced by a library
    private List<Map.Entry<LocalDateTime, Double>> parseResults(String json) {
        List<Map.Entry<LocalDateTime, Double>> entries = new ArrayList<>();

        // Locate the "results" array
        int resultsIdx = json.indexOf("\"results\"");
        if (resultsIdx == -1) {
            return entries;
        }

        int arrayStart = json.indexOf('[', resultsIdx);
        if (arrayStart == -1) {
            return entries;
        }

        // Walk through each object in the array
        int searchFrom = arrayStart;
        while (true) {
            int objStart = json.indexOf('{', searchFrom);
            if (objStart == -1) break;

            int objEnd = json.indexOf('}', objStart);
            if (objEnd == -1) break;

            String obj = json.substring(objStart, objEnd + 1);

            Double valueIncVat = extractDouble(obj, "value_inc_vat");
            String validFrom = extractString(obj, "valid_from");

            if (valueIncVat != null && validFrom != null) {
                LocalDateTime dateTime = LocalDateTime.parse(validFrom, DateTimeFormatter.ISO_DATE_TIME);
                entries.add(new AbstractMap.SimpleImmutableEntry<>(dateTime, valueIncVat));
            }

            searchFrom = objEnd + 1;
        }

        return entries;
    }

    // Extracts a quoted string value for the given key from a flat JSON object.
    private String extractString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;

        int colonIdx = json.indexOf(':', idx + search.length());
        if (colonIdx == -1) return null;

        int quoteStart = json.indexOf('"', colonIdx + 1);
        if (quoteStart == -1) return null;

        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteEnd == -1) return null;

        return json.substring(quoteStart + 1, quoteEnd);
    }

    // Extracts a numeric value for the given key from a flat JSON object.
    private Double extractDouble(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;

        int colonIdx = json.indexOf(':', idx + search.length());
        if (colonIdx == -1) return null;

        // Skips whitespace after colon
        int numStart = colonIdx + 1;
        while (numStart < json.length() && Character.isWhitespace(json.charAt(numStart))) {
            numStart++;
        }

        int numEnd = numStart;
        while (numEnd < json.length()
                && (Character.isDigit(json.charAt(numEnd))
                || json.charAt(numEnd) == '.'
                || json.charAt(numEnd) == '-')) {
            numEnd++;
        }

        if (numEnd == numStart) return null;

        try {
            return Double.parseDouble(json.substring(numStart, numEnd));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Formats a LocalDateTime into the ISO-8601 format expected by the API.
    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.toString();
    }
}