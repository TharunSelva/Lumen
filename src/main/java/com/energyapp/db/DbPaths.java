package com.energyapp.db;

import java.nio.file.Files;
import java.nio.file.Path;

public final class DbPaths {
    private DbPaths() {}

    /** SQLite DB file location (relative to project/app working directory). */
    public static Path dbFile() {
        return Path.of("data", "energy_tool.sqlite");
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