package com.energyapp.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Fetches half-hourly electricity unit rates from the Octopus Energy API.
 *
 * Uses period_from/period_to in ISO8601 UTC ("Z") format (recommended by Octopus docs). :contentReference[oaicite:2]{index=2}
 */
public class OctopusEnergyTariffService {

    // TODO: if you ever want this configurable, move to a settings table or app_meta.
    private static final String BASE_URL =
            "https://api.octopus.energy/v1/products/AGILE-24-10-01/electricity-tariffs/E-1R-AGILE-24-10-01-A/standard-unit-rates/";

    private static final int MAX_RESULTS_PER_REQUEST = 100; // Octopus price endpoints default to 100 per page :contentReference[oaicite:3]{index=3}
    private static final int MINUTES_PER_SLOT = 30;
    private static final long MAX_SLOTS_PER_REQUEST = MAX_RESULTS_PER_REQUEST; // 100 half-hours = 50 hours

    private static final DateTimeFormatter OCTOPUS_PARAM_FMT = DateTimeFormatter.ISO_INSTANT;

    private final HttpClient httpClient;

    public OctopusEnergyTariffService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    OctopusEnergyTariffService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /** A single half-hour rate record (p/kWh inc VAT) */
    public static final class UnitRate {
        public final OffsetDateTime validFromUtc;
        public final OffsetDateTime validToUtc;
        public final double pencePerKwhIncVat;

        public UnitRate(OffsetDateTime validFromUtc, OffsetDateTime validToUtc, double pencePerKwhIncVat) {
            this.validFromUtc = validFromUtc;
            this.validToUtc = validToUtc;
            this.pencePerKwhIncVat = pencePerKwhIncVat;
        }
    }

    /**
     * Fetch unit rates between [fromUtc, toUtc).
     * from/to MUST be UTC offsets, and will be sent as ISO8601 with trailing Z. :contentReference[oaicite:4]{index=4}
     */
    public List<UnitRate> getUnitRates(OffsetDateTime fromUtc, OffsetDateTime toUtc)
            throws IOException, InterruptedException {

        OffsetDateTime from = fromUtc.withOffsetSameInstant(ZoneOffset.UTC);
        OffsetDateTime to = toUtc.withOffsetSameInstant(ZoneOffset.UTC);

        List<UnitRate> all = new ArrayList<>();

        OffsetDateTime batchStart = from;
        while (batchStart.isBefore(to)) {
            OffsetDateTime batchEnd = batchStart.plus(MAX_SLOTS_PER_REQUEST * MINUTES_PER_SLOT, ChronoUnit.MINUTES);
            if (batchEnd.isAfter(to)) batchEnd = to;

            all.addAll(fetchBatch(batchStart, batchEnd));
            batchStart = batchEnd;
        }

        // Octopus results can come back newest-first; sort to make downstream logic easier
        all.sort(Comparator.comparing(r -> r.validFromUtc));
        return all;
    }

    private List<UnitRate> fetchBatch(OffsetDateTime fromUtc, OffsetDateTime toUtc)
            throws IOException, InterruptedException {

        String periodFrom = URLEncoder.encode(fromUtc.toInstant().format(OCTOPUS_PARAM_FMT), StandardCharsets.UTF_8);
        String periodTo = URLEncoder.encode(toUtc.toInstant().format(OCTOPUS_PARAM_FMT), StandardCharsets.UTF_8);

        String url = BASE_URL + "?period_from=" + periodFrom + "&period_to=" + periodTo;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Octopus API HTTP " + response.statusCode() + ": " + response.body());
        }

        return parseResults(response.body());
    }

    /**
     * Robust JSON parsing using JsonParser (no reflection + no brittle string searching).
     */
    private List<UnitRate> parseResults(String json) {
        List<UnitRate> out = new ArrayList<>();

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray results = root.getAsJsonArray("results");
        if (results == null) return out;

        for (JsonElement el : results) {
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();

            // value_inc_vat is p/kWh
            if (!obj.has("value_inc_vat") || obj.get("value_inc_vat").isJsonNull()) continue;
            double valueIncVat = obj.get("value_inc_vat").getAsDouble();

            // valid_from / valid_to are ISO8601 with timezone (often Z)
            String validFrom = obj.has("valid_from") && !obj.get("valid_from").isJsonNull()
                    ? obj.get("valid_from").getAsString() : null;
            String validTo = obj.has("valid_to") && !obj.get("valid_to").isJsonNull()
                    ? obj.get("valid_to").getAsString() : null;

            if (validFrom == null || validTo == null) continue;

            // OffsetDateTime correctly preserves Z/+00:00 offsets
            OffsetDateTime fromUtc = OffsetDateTime.parse(validFrom).withOffsetSameInstant(ZoneOffset.UTC);
            OffsetDateTime toUtc = OffsetDateTime.parse(validTo).withOffsetSameInstant(ZoneOffset.UTC);

            out.add(new UnitRate(fromUtc, toUtc, valueIncVat));
        }

        return out;
    }

    /**
     * Helper for your app: get a UTC period window aligned to 30-min boundaries.
     * (Not required by Octopus, but it matches the half-hour rate buckets.)
     */
    public static OffsetDateTime roundDownToHalfHourUtc(OffsetDateTime t) {
        OffsetDateTime utc = t.withOffsetSameInstant(ZoneOffset.UTC).withSecond(0).withNano(0);
        int minute = utc.getMinute();
        int rounded = minute < 30 ? 0 : 30;
        return utc.withMinute(rounded);
    }
}