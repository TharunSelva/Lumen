package com.energyapp.controller;

import com.energyapp.db.HomeProfileRepository;
import com.energyapp.model.Geocode;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsControllerTest {

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void buildSettingsGreetingUsesStoredFirstName() {
        assertEquals("Lumen settings for Alex", SettingsController.buildSettingsGreeting(" Alex "));
        assertEquals("Lumen settings", SettingsController.buildSettingsGreeting(" "));
    }

    @Test
    void buildProfileLocationUsesSavedCityWhenAvailable() {
        assertEquals(
                "Home city: Southampton. Forecasts and solar estimates use this location.",
                SettingsController.buildProfileLocation("Southampton")
        );
        assertTrue(SettingsController.buildProfileLocation(" ").contains("Add your city"));
    }

    @Test
    void faqEntriesStayAppSpecific() {
        List<SettingsController.FaqEntry> entries = SettingsController.faqEntries();

        assertEquals(5, entries.size());
        assertTrue(entries.stream().anyMatch(entry -> entry.answer().contains("forecast summaries and solar outlook")));
        assertTrue(entries.stream().anyMatch(entry -> entry.answer().contains("dark mode")));
    }

    @Test
    void bugReportTemplateIncludesSavedProfileContext() {
        HomeProfileRepository.HomeProfileSnapshot profile = new HomeProfileRepository.HomeProfileSnapshot(
                "Alex",
                "Southampton",
                new Geocode(50.9097, -1.4044)
        );

        String template = SettingsController.buildBugReportTemplate(
                profile,
                LocalDateTime.of(2026, 4, 24, 16, 45)
        );

        assertTrue(template.contains("Profile: Alex"));
        assertTrue(template.contains("Saved city: Southampton"));
        assertTrue(template.contains("Created: 24 Apr 2026, 16:45"));
    }

    @Test
    void settingsViewLoadsSuccessfully() throws Exception {
        CountDownLatch loadLatch = new CountDownLatch(1);
        Parent[] loadedRoot = new Parent[1];

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/energyapp/view/Settings.fxml"));
                loadedRoot[0] = loader.load();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                loadLatch.countDown();
            }
        });

        assertTrue(loadLatch.await(2, TimeUnit.SECONDS));
        assertNotNull(loadedRoot[0]);
    }
}
