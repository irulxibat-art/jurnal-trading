package com.irul.trading.util;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;

public class TradeHttpServer {
    private static final int PORT = 8081;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/trade", new TradeHandler());
        server.createContext("/api/account", new AccountHandler());
        server.createContext("/api/market", new MarketDataHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("HTTP Server listening on port " + PORT);
    }

    static class TradeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                        .lines().reduce("", (a, b) -> a + b);
                System.out.println("Trade received: " + body);
                boolean ok = false;
                try {
                    JSONObject obj = new JSONObject(body);
                    String action = obj.getString("action");
                    if ("OPEN".equals(action)) {
                        String symbol = obj.getString("symbol");
                        double price = obj.getDouble("price");
                        double volume = obj.getDouble("volume");
                        long positionId = obj.getLong("positionId");
                        String type = obj.getString("type");
                        // Cegah duplikat
                        String checkSql = "SELECT COUNT(*) FROM trades WHERE position_id = ?";
                        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement ps = conn.prepareStatement(checkSql)) {
                            ps.setLong(1, positionId);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next() && rs.getInt(1) > 0) {
                                System.out.println("Duplicate positionId " + positionId + ", skipping INSERT.");
                                String resp = "OK";
                                exchange.sendResponseHeaders(200, resp.length());
                                exchange.getResponseBody().write(resp.getBytes());
                                exchange.getResponseBody().close();
                                return;
                            }
                        }
                        String sql = "INSERT INTO trades (tanggal_buka, aset, tipe, entry_price, lot_size, position_id, status) VALUES (datetime('now'), ?, ?, ?, ?, ?, 'OPEN')";
                        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, symbol);
                            ps.setString(2, type);
                            ps.setDouble(3, price);
                            ps.setDouble(4, volume);
                            ps.setLong(5, positionId);
                            ps.executeUpdate();
                            System.out.println("OPEN trade inserted: positionId=" + positionId);
                        }
                        ok = true;
                    } else if ("CLOSE".equals(action)) {
                        long positionId = obj.getLong("positionId");
                        double closePrice = obj.getDouble("price");
                        double profitFromMT5 = obj.has("profit") ? obj.getDouble("profit") : 0.0;
                        System.out.println("CLOSE request: positionId=" + positionId + ", closePrice=" + closePrice + ", profit=" + profitFromMT5);
                        
                        // *** PERBAIKAN: Hilangkan filter tipe ***
                        String updateSql = "UPDATE trades SET tanggal_tutup = datetime('now'), exit_price = ?, profit_loss = ?, status = 'CLOSED' WHERE position_id = ? AND status = 'OPEN'";
                        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement ps = conn.prepareStatement(updateSql)) {
                            ps.setDouble(1, closePrice);
                            ps.setDouble(2, profitFromMT5);
                            ps.setLong(3, positionId);
                            int rows = ps.executeUpdate();
                            System.out.println("UPDATE affected rows: " + rows);
                            if (rows > 0) {
                                ok = true;
                            } else {
                                // Debug: cek status trade dengan positionId tersebut
                                String debugSql = "SELECT status FROM trades WHERE position_id = ?";
                                try (PreparedStatement ps2 = conn.prepareStatement(debugSql)) {
                                    ps2.setLong(1, positionId);
                                    ResultSet rs2 = ps2.executeQuery();
                                    if (rs2.next()) {
                                        System.out.println("Existing trade status: " + rs2.getString("status"));
                                    } else {
                                        System.out.println("No trade found for positionId " + positionId);
                                    }
                                }
                                ok = false;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ok = false;
                }
                String resp = ok ? "OK" : "ERROR";
                exchange.sendResponseHeaders(ok ? 200 : 500, resp.length());
                exchange.getResponseBody().write(resp.getBytes());
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
            exchange.getResponseBody().close();
        }
    }

    // ===================== HANDLER AKUN =====================
    static class AccountHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                        .lines().reduce("", (a, b) -> a + b);
                System.out.println("Account: " + body);
                try {
                    JSONObject obj = new JSONObject(body);
                    if ("account".equals(obj.getString("type"))) {
                        String login = obj.getString("login");
                        double bal = obj.getDouble("balance");
                        double eq = obj.getDouble("equity");
                        double margin = obj.getDouble("margin");
                        double free = obj.getDouble("freeMargin");
                        String server = obj.getString("server");
                        String sql = "INSERT OR REPLACE INTO account_info (login, balance, equity, margin, free_margin, server, last_update) VALUES (?,?,?,?,?,?,datetime('now'))";
                        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, login);
                            ps.setDouble(2, bal);
                            ps.setDouble(3, eq);
                            ps.setDouble(4, margin);
                            ps.setDouble(5, free);
                            ps.setString(6, server);
                            ps.executeUpdate();
                        }
                        exchange.sendResponseHeaders(200, 0);
                    } else {
                        exchange.sendResponseHeaders(400, -1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1);
                }
                exchange.getResponseBody().close();
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.getResponseBody().close();
            }
        }
    }

    // ===================== HANDLER MARKET DATA =====================
    static class MarketDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                        .lines().reduce("", (a, b) -> a + b);
                System.out.println("Market: " + body);
                try {
                    JSONObject obj = new JSONObject(body);
                    if ("MARKET".equals(obj.getString("action"))) {
                        String symbol = obj.getString("symbol");
                        double bid = obj.getDouble("bid");
                        double ask = obj.getDouble("ask");
                        DatabaseHelper.saveMarketData(symbol, bid, ask);
                        exchange.sendResponseHeaders(200, 0);
                    } else {
                        exchange.sendResponseHeaders(400, -1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1);
                }
                exchange.getResponseBody().close();
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.getResponseBody().close();
            }
        }
    }
}