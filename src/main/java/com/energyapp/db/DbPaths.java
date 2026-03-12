package com.energyapp.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class DbPaths {
    private DbPaths() {}

    /** SQLite DB file location (stored safely in the User's Home directory). */
    public static Path dbFile() {
        String userHome = System.getProperty("user.home");
        // Creates a hidden folder in the home directory: e.g., C:\Users\Marker\.energyapp\data\
        return Paths.get(userHome, ".energyapp", "data", "energy_tool.sqlite");
    }

    public static void ensureDataDirExists() {
        try {
            Path parent = dbFile().getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create data/ directory for DB", e);
        }
    }
}