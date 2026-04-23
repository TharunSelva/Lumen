package com.energyapp.db;

import com.energyapp.model.QuickTip;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class QuickTipRepository {

    private static final String TEMPLATE_DB_RESOURCE = "/com/energy_template_db.sqlite";

    public List<QuickTip> fetchAllTips() {
        List<QuickTip> runtimeTips = fetchTipsFromPath(DbPaths.dbFile());
        if (runtimeTips.size() >= 10) {
            return runtimeTips;
        }

        List<QuickTip> templateTips = fetchTipsFromTemplate();
        if (!templateTips.isEmpty()) {
            return templateTips;
        }

        if (!runtimeTips.isEmpty()) {
            return runtimeTips;
        }

        return List.of();
    }

    private List<QuickTip> fetchTipsFromTemplate() {
        Path tempDbPath = copyTemplateDbToTempFile();
        try {
            return fetchTipsFromPath(tempDbPath);
        } finally {
            try {
                Files.deleteIfExists(tempDbPath);
            } catch (IOException ignored) {
            }
        }
    }

    private List<QuickTip> fetchTipsFromPath(Path dbPath) {
        String sql = """
            SELECT tip_id, title, body
            FROM quick_tip_catalog
            ORDER BY tip_id
            """;

        try (Connection conn = openConnection(dbPath);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<QuickTip> tips = new ArrayList<>();
            while (rs.next()) {
                tips.add(new QuickTip(
                        rs.getInt("tip_id"),
                        rs.getString("title"),
                        rs.getString("body")
                ));
            }
            return tips;
        } catch (SQLException e) {
            return List.of();
        }
    }

    private Connection openConnection(Path dbPath) throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    private Path copyTemplateDbToTempFile() {
        try (InputStream in = QuickTipRepository.class.getResourceAsStream(TEMPLATE_DB_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing template DB resource: " + TEMPLATE_DB_RESOURCE);
            }

            Path tempFile = Files.createTempFile("energy-template", ".sqlite");
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("Failed to access template DB for quick tips", e);
        }
    }
}
