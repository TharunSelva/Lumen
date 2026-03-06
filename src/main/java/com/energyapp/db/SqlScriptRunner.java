package com.energyapp.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public final class SqlScriptRunner {
    private SqlScriptRunner() {}

    public static void runResource(Connection conn, String resourcePath) {
        try (InputStream in = SqlScriptRunner.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new IllegalStateException("Missing resource: " + resourcePath);

            String raw = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            // Strip -- comments
            String noLineComments = raw.replaceAll("(?m)--.*?$", "");
            // Strip /* */ comments
            String cleaned = noLineComments.replaceAll("(?s)/\\*.*?\\*/", "");

            String[] statements = cleaned.split(";");
            try (Statement s = conn.createStatement()) {
                for (String stmt : statements) {
                    String sql = stmt.trim();
                    if (!sql.isEmpty()) s.execute(sql);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed running SQL resource: " + resourcePath, e);
        }
    }
}