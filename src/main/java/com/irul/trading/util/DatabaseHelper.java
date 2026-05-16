package com.irul.trading.util;

import java.sql.*;

public class DatabaseHelper {

    private static final String DB_URL =
        "jdbc:sqlite:D:\\AplikasiTrading\\data\\trading.db";

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
            + " key TEXT PRIMARY KEY, value TEXT);";

        String accountTable = "CREATE TABLE IF NOT EXISTS account_info ("
            + " login TEXT PRIMARY KEY,"
            + " balance REAL,"
            + " equity REAL,"
            + " margin REAL,"
            + " free_margin REAL,"
            + " server TEXT,"
            + " currency TEXT DEFAULT 'USD',"
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
            addColumnIfNotExists(conn, "account_info", "currency",    "TEXT DEFAULT 'USD'");
            addColumnIfNotExists(conn, "account_info", "margin",      "REAL");
            addColumnIfNotExists(conn, "account_info", "free_margin", "REAL");
            System.out.println("Database siap.");
        } catch (SQLException e) {
            System.err.println("Gagal buat tabel: " + e.getMessage());
        }
    }

    private static void addColumnIfNotExists(
            Connection conn, String table, String column, String definition) {
        try {
            boolean exists = false;
            ResultSet rs = conn.createStatement()
                .executeQuery("PRAGMA table_info(" + table + ")");
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) { exists = true; break; }
            }
            if (!exists) {
                conn.createStatement()
                    .execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                System.out.println("[DB] Kolom ditambahkan: " + table + "." + column);
            }
        } catch (SQLException e) {
            System.err.println("[DB] Gagal migrasi " + column + ": " + e.getMessage());
        }
    }

    // MARKET DATA
    public static void saveMarketData(String symbol, double bid, double ask) {
        String sql = "INSERT INTO market_data (symbol, bid, ask) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbol); ps.setDouble(2, bid); ps.setDouble(3, ask);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static double getLatestAsk(String symbol) {
        String sql = "SELECT ask FROM market_data WHERE UPPER(symbol)=UPPER(?) ORDER BY id DESC LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbol);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("ask");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    public static String getLatestMarketSummaryByLike(String symbolPattern) {
        String sql = "SELECT symbol, ask, bid FROM market_data WHERE UPPER(symbol) LIKE UPPER(?) ORDER BY id DESC LIMIT 1";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbolPattern + "%");
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("symbol") + " | bid: " + rs.getDouble("bid") + " | ask: " + rs.getDouble("ask");
        } catch (SQLException e) { e.printStackTrace(); }
        return "";
    }

    public static String getRecentPriceHistory(String symbol, int limit) {
        String sql = "SELECT ask, bid, timestamp FROM market_data WHERE UPPER(symbol)=UPPER(?) ORDER BY id DESC LIMIT ?";
        java.util.List<String> rows = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder("Riwayat harga " + symbol + " (" + limit + " data, lama->baru):\n");
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, symbol); ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(String.format("  [%s] Ask: %.2f | Bid: %.2f",
                    rs.getString("timestamp"), rs.getDouble("ask"), rs.getDouble("bid")));
            }
        } catch (SQLException e) { return "Gagal ambil riwayat harga: " + e.getMessage(); }
        if (rows.isEmpty()) return "Riwayat harga " + symbol + " belum tersedia. Pastikan EA MT5 berjalan.";
        java.util.Collections.reverse(rows);
        for (String r : rows) sb.append(r).append("\n");
        try {
            double fa = Double.parseDouble(rows.get(0).split("Ask: ")[1].split(" \\|")[0].trim());
            double la = Double.parseDouble(rows.get(rows.size()-1).split("Ask: ")[1].split(" \\|")[0].trim());
            double d = la - fa;
            sb.append(String.format("\nRingkasan: %.2f -> %.2f | Delta: %+.2f | Tren: %s\n",
                fa, la, d, d > 0 ? "NAIK" : d < 0 ? "TURUN" : "SIDEWAYS"));
        } catch (Exception ignored) {}
        return sb.toString();
    }

    // AKUN
    public static double getLatestBalance()    { return getAccountDouble("balance"); }
    public static double getLatestEquity()     { return getAccountDouble("equity"); }
    public static double getLatestMargin()     { return getAccountDouble("margin"); }
    public static double getLatestFreeMargin() { return getAccountDouble("free_margin"); }

    public static String getAccountCurrency() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT currency FROM account_info ORDER BY last_update DESC LIMIT 1")) {
            if (rs.next()) {
                String c = rs.getString("currency");
                return (c != null && !c.isEmpty()) ? c.toUpperCase() : "USD";
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return "USD";
    }

    private static double getAccountDouble(String column) {
        String sql = "SELECT " + column + " FROM account_info ORDER BY last_update DESC LIMIT 1";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(column);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    public static int getOpenTradesCount() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM trades WHERE status='OPEN'")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    // STATISTIK
    public static double getWinRate() {
        String sql = "SELECT COUNT(*) as total, SUM(CASE WHEN profit_loss>0 THEN 1 ELSE 0 END) as win FROM trades WHERE status='CLOSED'";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) { int t = rs.getInt("total"), w = rs.getInt("win"); return t==0?0:(double)w/t*100; }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public static double getProfitFactor() {
        String sql = "SELECT SUM(CASE WHEN profit_loss>0 THEN profit_loss ELSE 0 END) as gp, SUM(CASE WHEN profit_loss<0 THEN profit_loss ELSE 0 END) as gl FROM trades WHERE status='CLOSED'";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) { double gp=rs.getDouble("gp"), gl=Math.abs(rs.getDouble("gl")); return gl==0?0:gp/gl; }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public static double getMaxDrawdown() {
        String sql = "SELECT profit_loss FROM trades WHERE status='CLOSED' ORDER BY tanggal_buka";
        double cum=0,peak=0,maxDD=0;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) { cum+=rs.getDouble("profit_loss"); if(cum>peak)peak=cum; double dd=peak-cum; if(dd>maxDD)maxDD=dd; }
        } catch (SQLException e) { e.printStackTrace(); }
        return maxDD;
    }

    public static int getTotalTrades() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM trades WHERE status='CLOSED'")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public static String getPerformanceSummary() {
        return "=== Performa Akun Trading ===\n"
            + String.format("- Currency    : %s\n",    getAccountCurrency())
            + String.format("- Balance     : %.2f\n",  getLatestBalance())
            + String.format("- Equity      : %.2f\n",  getLatestEquity())
            + String.format("- Win Rate    : %.2f%%\n", getWinRate())
            + String.format("- Profit Factor: %.2f\n", getProfitFactor())
            + String.format("- Max Drawdown: %.2f\n",  getMaxDrawdown())
            + String.format("- Total Closed: %d\n",    getTotalTrades())
            + String.format("- Open Now    : %d\n",    getOpenTradesCount());
    }

    public static String getRecentTrades(int limit) {
        StringBuilder sb = new StringBuilder(limit + " transaksi terakhir:\n");
        String sql = "SELECT id,tanggal_buka,aset,tipe,entry_price,exit_price,profit_loss,status FROM trades ORDER BY id DESC LIMIT " + limit;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            int i=1;
            while (rs.next()) {
                sb.append(i++).append(". ").append(rs.getString("tanggal_buka")).append(" | ")
                  .append(rs.getString("aset")).append(" | ").append(rs.getString("tipe"))
                  .append(" | Entry: ").append(rs.getDouble("entry_price"));
                if ("CLOSED".equals(rs.getString("status")))
                    sb.append(" | Exit: ").append(rs.getDouble("exit_price"))
                      .append(" | P/L: ").append(rs.getDouble("profit_loss"));
                else sb.append(" | OPEN");
                sb.append("\n");
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return sb.toString();
    }
}
