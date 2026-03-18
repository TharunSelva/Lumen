package com.energyapp.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

public final class Sqlite {
    private Sqlite() {}

    public static Connection openConnection() throws SQLException {
        String url = "jdbc:sqlite:" + DbPaths.dbFile().toAbsolutePath();
        Connection c = DriverManager.getConnection(url);

        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
            s.execute("PRAGMA busy_timeout = 5000");
        }
        return c;
    }
}