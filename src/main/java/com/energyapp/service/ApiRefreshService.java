package com.energyapp.service;

import com.energyapp.db.Sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically refreshes API caches while the app is running.
 * Writes into SQLite; controllers read from SQLite (so UI stays decoupled from API calls).
 */
public final class ApiRefreshService {

    // Timestamp format stored in app_meta.meta_value (e.g., "2026-03-05 12:34:56")
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Single background thread that runs refresh jobs on a fixed schedule
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Cache TTLs (if the last refresh is newer than this, skip the API call)
    private static final Duration CARBON_TTL = Duration.ofMinutes(25);
    private static final Duration OCTOPUS_TTL = Duration.ofMinutes(55);
    private static final Duration SOLAR_TTL  = Duration.ofMinutes(170);

    // Simple guard to avoid double-starting the scheduler
    private volatile boolean started = false;

    public void start() {
        if (started) return;
        started = true;

        // Run once quickly (initial delay), then periodically (period).
        // NOTE: TimeUnit.SECONDS => the numbers below are seconds.
        scheduler.scheduleAtFixedRate(this::refreshCarbonAndMixSafe, 5,  30 * 60L, TimeUnit.SECONDS);   // every 30 min
        scheduler.scheduleAtFixedRate(this::refreshOctopusSafe,     10, 60 * 60L, TimeUnit.SECONDS);   // every 60 min
        scheduler.scheduleAtFixedRate(this::refreshSolarSafe,       15, 180 * 60L, TimeUnit.SECONDS);  // every 180 min
    }

    public void stop() {
        // Stop all scheduled tasks immediately (called when app exits)
        scheduler.shutdownNow();
    }

    private void refreshCarbonAndMixSafe() {
        try (Connection conn = Sqlite.openConnection()) {
            // If cache is still fresh, do nothing
            if (isFresh(conn, "last_carbon_cache_at", CARBON_TTL)) return;

            // Delegate to DB bootstrap refresh logic (API call + DB writes)
            com.energyapp.db.DbBootstrap.refreshCarbonAndMix(conn);

            // Record refresh time in app_meta so future runs can skip until TTL expires
            upsertMeta(conn, "last_carbon_cache_at", LocalDateTime.now().format(TS_FMT));
            System.out.println("Refreshed carbon/mix");
        } catch (Exception e) {
            // Never crash the scheduler thread: log and keep running future refreshes
            System.err.println("Periodic refresh failed (carbon/mix)");
            e.printStackTrace();
        }
    }

    private void refreshOctopusSafe() {
        try (Connection conn = Sqlite.openConnection()) {
            if (isFresh(conn, "last_octopus_cache_at", OCTOPUS_TTL)) return;

            com.energyapp.db.DbBootstrap.refreshOctopus(conn);

            upsertMeta(conn, "last_octopus_cache_at", LocalDateTime.now().format(TS_FMT));
            System.out.println("Refreshed octopus");
        } catch (Exception e) {
            System.err.println("Periodic refresh failed (octopus)");
            e.printStackTrace();
        }
    }

    private void refreshSolarSafe() {
        try (Connection conn = Sqlite.openConnection()) {
            if (isFresh(conn, "last_solar_cache_at", SOLAR_TTL)) return;

            com.energyapp.db.DbBootstrap.refreshSolar(conn);

            upsertMeta(conn, "last_solar_cache_at", LocalDateTime.now().format(TS_FMT));
            System.out.println("Refreshed solar");
        } catch (Exception e) {
            System.err.println("Periodic refresh failed (solar)");
            e.printStackTrace();
        }
    }

    /**
     * Checks app_meta to see whether a cache key was refreshed within the TTL window.
     * Returns false if the key doesn't exist, value is blank, or parsing fails.
     */
    private boolean isFresh(Connection conn, String metaKey, Duration ttl) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT meta_value FROM app_meta WHERE meta_key=?"
        )) {
            ps.setString(1, metaKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;

                String last = rs.getString(1);
                if (last == null || last.isBlank()) return false;

                LocalDateTime lastTime = LocalDateTime.parse(last, TS_FMT);
                return lastTime.isAfter(LocalDateTime.now().minus(ttl));
            }
        } catch (Exception e) {
            // If anything goes wrong, treat as stale so we attempt a refresh next time
            return false;
        }
    }

    /**
     * Inserts or updates a key/value pair in app_meta.
     * Requires app_meta.meta_key to be UNIQUE or PRIMARY KEY for ON CONFLICT to work.
     */
    private void upsertMeta(Connection conn, String key, String value) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO app_meta(meta_key, meta_value) VALUES(?,?)
            ON CONFLICT(meta_key) DO UPDATE SET meta_value=excluded.meta_value
            """)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }
}