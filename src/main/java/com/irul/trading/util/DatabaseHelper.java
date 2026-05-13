package com.irul.trading.util;

import java.sql.*;

public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:D:\\AplikasiTrading\\data\\trading.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void createTables() {
        String tradesTable = "CREATE TABLE IF NOT EXISTS trades ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " tanggal_buka TEXT NOT NULL,"
                + " tanggal_tutup TEXT,"
                + " aset TEXT NOT NULL,"
                + " tipe TEXT NOT NULL,"
                + " entry_price REAL NOT NULL,"
                + " exit_price REAL,"
                + " lot_size REAL NOT NULL,"
                + " stop_loss REAL,"
                + " take_profit REAL,"
                + " profit_loss REAL,"
                + " status TEXT DEFAULT 'OPEN',"
                + " order_id INTEGER,"
                + " position_id INTEGER"
                + ");";

        String settingsTable = "CREATE TABLE IF NOT EXISTS settings ("
                + " key TEXT PRIMARY KEY,"
                + " value TEXT"
                + ");";

        String accountTable = "CREATE TABLE IF NOT EXISTS account_info ("
                + " login TEXT PRIMARY KEY,"
                + " balance REAL,"
                + " equity REAL,"
                + " margin REAL,"
                + " free_margin REAL,"
                + " server TEXT,"
                + " last_update TEXT"
                + ");";

        String marketDataTable = "CREATE TABLE IF NOT EXISTS market_data ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " symbol TEXT NOT NULL,"
                + " bid REAL,"
                + " ask REAL,"
                + " timestamp TEXT DEFAULT (datetime('now'))"
                + ");";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(tradesTable);
            stmt.execute(settingsTable);
            stmt.execute(accountTable);
            stmt.execute(marketDataTable);
            System.out.println("Database siap.");
        } catch (SQLException e) {
            System.err.println("Gagal buat tabel: " + e.getMessage());
        }
    }

    // ========== MARKET DATA ==========
    public static void saveMarketData(String symbol, double bid, double ask) {
        String sql = "INSERT INTO market_data (symbol, bid, ask) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, symbol);
            pstmt.setDouble(2, bid);
            pstmt.setDouble(3, ask);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static double getLatestAsk(String symbol) {
        String sql = "SELECT ask FROM market_data WHERE UPPER(symbol) = UPPER(?) ORDER BY id DESC LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, symbol);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble("ask");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    public static String getLatestMarketSummaryByLike(String symbolPattern) {
        String sql = "SELECT symbol, ask, bid FROM market_data WHERE UPPER(symbol) LIKE UPPER(?) ORDER BY id DESC LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, symbolPattern + "%");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("symbol") + " | bid: " + rs.getDouble("bid") + " | ask: " + rs.getDouble("ask");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return "";
    }

    // ========== AKUN ==========
    public static double getLatestBalance() {
        String sql = "SELECT balance FROM account_info ORDER BY last_update DESC LIMIT 1";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble("balance");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    public static double getLatestEquity() {
        String sql = "SELECT equity FROM account_info ORDER BY last_update DESC LIMIT 1";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble("equity");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    public static int getOpenTradesCount() {
        String sql = "SELECT COUNT(*) FROM trades WHERE status = 'OPEN'";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // ========== STATISTIK ==========
    public static double getWinRate() {
        String sql = "SELECT COUNT(*) as total, SUM(CASE WHEN profit_loss > 0 THEN 1 ELSE 0 END) as win FROM trades WHERE status = 'CLOSED'";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int total = rs.getInt("total");
                int win = rs.getInt("win");
                return total == 0 ? 0 : (double) win / total * 100;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public static double getProfitFactor() {
        String sql = "SELECT SUM(CASE WHEN profit_loss > 0 THEN profit_loss ELSE 0 END) as grossProfit, SUM(CASE WHEN profit_loss < 0 THEN profit_loss ELSE 0 END) as grossLoss FROM trades WHERE status = 'CLOSED'";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                double grossProfit = rs.getDouble("grossProfit");
                double grossLoss = Math.abs(rs.getDouble("grossLoss"));
                return grossLoss == 0 ? 0 : grossProfit / grossLoss;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public static double getMaxDrawdown() {
        String sql = "SELECT tanggal_buka, profit_loss FROM trades WHERE status = 'CLOSED' ORDER BY tanggal_buka";
        double cumulative = 0, peak = 0, maxDD = 0;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cumulative += rs.getDouble("profit_loss");
                if (cumulative > peak) peak = cumulative;
                double dd = peak - cumulative;
                if (dd > maxDD) maxDD = dd;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return maxDD;
    }

    public static int getTotalTrades() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM trades WHERE status = 'CLOSED'")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public static String getPerformanceSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Performa Trading:\n");
        sb.append(String.format("- Balance: Rp %.2f\n", getLatestBalance()));
        sb.append(String.format("- Equity: Rp %.2f\n", getLatestEquity()));
        sb.append(String.format("- Win Rate: %.2f%%\n", getWinRate()));
        sb.append(String.format("- Profit Factor: %.2f\n", getProfitFactor()));
        sb.append(String.format("- Max Drawdown: Rp %.2f\n", getMaxDrawdown()));
        sb.append(String.format("- Total Closed Trades: %d\n", getTotalTrades()));
        sb.append(String.format("- Open Positions: %d\n", getOpenTradesCount()));
        return sb.toString();
    }

    public static String getRecentTrades(int limit) {
        StringBuilder sb = new StringBuilder();
        sb.append(limit).append(" transaksi terakhir:\n");
        String sql = "SELECT id, tanggal_buka, aset, tipe, entry_price, exit_price, profit_loss, status FROM trades ORDER BY id DESC LIMIT " + limit;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            int i = 1;
            while (rs.next()) {
                sb.append(i++).append(". ")
                  .append(rs.getString("tanggal_buka")).append(" | ")
                  .append(rs.getString("aset")).append(" | ")
                  .append(rs.getString("tipe")).append(" | Entry: ").append(rs.getDouble("entry_price"));
                if ("CLOSED".equals(rs.getString("status"))) {
                    sb.append(" | Exit: ").append(rs.getDouble("exit_price"));
                    sb.append(" | P/L: ").append(rs.getDouble("profit_loss")).append("\n");
                } else {
                    sb.append(" | Status: OPEN\n");
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return sb.toString();
    }
}