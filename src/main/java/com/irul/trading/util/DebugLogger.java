package com.irul.trading.util;

import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.UUID;

public class DebugLogger {
    private static final String LOG_PATH = "debug-e6dd8c.log";
    private static final String FALLBACK_LOG_PATH = "debug-e6dd8c.log";
    private static final String DB_URL = "jdbc:sqlite:D:\\AplikasiTrading\\data\\trading.db";
    private static final String SESSION_ID = "e6dd8c";

    public static void log(String runId, String hypothesisId, String location, String message, JSONObject data) {
        JSONObject payload = new JSONObject();
        payload.put("sessionId", SESSION_ID);
        payload.put("id", "log_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8));
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("runId", runId);
        payload.put("hypothesisId", hypothesisId);
        payload.put("location", location);
        payload.put("message", message);
        payload.put("data", data == null ? new JSONObject() : data);
        writeLine(LOG_PATH, payload.toString());
        writeLine(FALLBACK_LOG_PATH, payload.toString());
        writeDb(payload);
    }

    private static void writeLine(String path, String line) {
        try (FileWriter fw = new FileWriter(path, true)) {
            fw.write(line);
            fw.write(System.lineSeparator());
        } catch (IOException e) {
            System.err.println("DebugLogger write failed for " + path + ": " + e.getMessage());
        }
    }

    private static void writeDb(JSONObject payload) {
        String createSql = "CREATE TABLE IF NOT EXISTS debug_logs (id TEXT PRIMARY KEY, session_id TEXT, run_id TEXT, hypothesis_id TEXT, location TEXT, message TEXT, data TEXT, timestamp INTEGER)";
        String insertSql = "INSERT INTO debug_logs (id, session_id, run_id, hypothesis_id, location, message, data, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement createPs = conn.prepareStatement(createSql);
             PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
            createPs.execute();
            insertPs.setString(1, payload.optString("id"));
            insertPs.setString(2, payload.optString("sessionId"));
            insertPs.setString(3, payload.optString("runId"));
            insertPs.setString(4, payload.optString("hypothesisId"));
            insertPs.setString(5, payload.optString("location"));
            insertPs.setString(6, payload.optString("message"));
            insertPs.setString(7, payload.optJSONObject("data") == null ? "{}" : payload.getJSONObject("data").toString());
            insertPs.setLong(8, payload.optLong("timestamp"));
            insertPs.executeUpdate();
        } catch (Exception e) {
            System.err.println("DebugLogger DB write failed: " + e.getMessage());
        }
    }
}
