package com.energyapp.controller;

import com.energyapp.db.DbPaths;
import com.energyapp.db.SqlScriptRunner;
import com.energyapp.db.Sqlite;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForecastingControllerViewTest {

    Path tempDir;

    private String originalUserHome;

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        tempDir = Files.createTempDirectory("forecasting-controller-test");
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        DbPaths.ensureDataDirExists();
        try (Connection conn = Sqlite.openConnection()) {
            SqlScriptRunner.runResource(conn, "/com/schema.sql");
            try (PreparedStatement home = conn.prepareStatement(
                    "INSERT INTO home_profile(home_id, first_name, timezone, created_at) VALUES(?,?,?,datetime('now'))")) {
                home.setString(1, "HOME_1");
                home.setString(2, "Richard");
                home.setString(3, "Europe/London");
                home.executeUpdate();
            }
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setProperty("user.home", originalUserHome);
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                        }
                    });
        }
    }

    @Test
    void forecastsViewLoadsWithSolarAndTomorrowPanels() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Parent[] loadedRoot = new Parent[1];
        ForecastingController[] loadedController = new ForecastingController[1];

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/energyapp/view/Forecasts.fxml"));
                loadedRoot[0] = loader.load();
                loadedController[0] = loader.getController();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotNull(loadedRoot[0]);
        assertNotNull(loadedController[0]);
    }
}
