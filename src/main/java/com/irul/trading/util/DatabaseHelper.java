package com.irul.trading.util;

import java.sql.*;

/**
 * DatabaseHelper — satu-satunya pintu masuk ke SQLite.
 *
 * Prinsip yang diterapkan:
 *  - Single Responsibility: hanya urusan DB, tidak ada logika bisnis.
 *  - Semua query menggunakan PreparedStatement untuk mencegah SQL Injection.
 *  - Tidak ada string concatenation langsung pada WHERE clause yang menerima input user.
 */
public class DatabaseHelper {

    // -------------------------------------------------------------------------
    // Koneksi
    // -------------------------------------------------------------------------

    private static final String DB_URL = "jdbc:sqlite:D:\\AplikasiTrading\\data\\trading.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // -------------------------------------------------------------------------
    // Inisialisasi tabel
    // -------------------------------------------------------------------------

    public static void createTables() {
        String tradesTable = "CREATE TABLE IF NOT EXISTS trades ("
                + " id            INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " tanggal_buka  TEXT    NOT NULL,"
                + " tanggal_tutup TEXT,"
                + " aset          TEXT    NOT NULL,"
                + " tipe          TEXT    NOT NULL,"
                + " entry_price   REAL    NOT NULL,"
                + " exit_price    REAL,"
                + " lot_size      REAL    NOT NULL,"
                + " stop_loss     REAL,"
                + " take_profit   REAL,"
                + " profit_loss   REAL,"
                + " status        TEXT    DEFAULT 'OPEN',"
                + " order_id      INTEGER,"
                + " position_id   INTEGER"
                + ");";

        String settingsTable = "CREATE TABLE IF NOT EXISTS settings ("
                + " key   TEXT PRIMARY KEY,"
                + " value TEXT"
                + ");";

        String accountTable = "CREATE TABLE IF NOT EXISTS account_info ("
                + " login        TEXT PRIMARY KEY,"
                + " balance      REAL,"
                + " equity       REAL,"
                + " margin       REAL,"
                + " free_margin  REAL,"
                + " server       TEXT,"
                + " last_update  TEXT"
                + ");";

        String marketDataTable = "CREATE TABLE IF NOT EXISTS market_data ("
                + " id        INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " symbol    TEXT NOT NULL,"
                + " bid       REAL,"
                + " ask       REAL,"
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

    // =========================================================================
    // MARKET DATA
    // =========================================================================

    /**
     * Menyimpan satu tick harga dari EA MT5.
     *
     * @param symbol Nama simbol, e.g. "XAUUSDm"
     * @param bid    Harga bid
     * @param ask    Harga ask
     */
    public static void saveMarketData(String symbol, double bid, double ask) {
        String sql = "INSERT INTO market_data (symbol, bid, ask) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, symbol);
            pstmt.setDouble(2, bid);
            pstmt.setDouble(3, ask);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Mengambil harga ask terbaru untuk suatu simbol.
     *
     * @param symbol Nama simbol (case-insensitive)
     * @return harga ask terakhir, atau 0.0 jika belum ada data
     */
    public static double getLatestAsk(String symbol) {
        String sql = "SELECT ask FROM market_data "
                   + "WHERE UPPER(symbol) = UPPER(?) "
                   + "ORDER BY id DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, symbol);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble("ask");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    /**
     * Mengambil ringkasan satu baris market data (bid + ask) berdasarkan prefix simbol.
     * Cocok untuk tampilan cepat, bukan untuk analisis.
     *
     * @param symbolPattern Prefix simbol, e.g. "XAUUSD"
     * @return String ringkasan atau string kosong jika tidak ada data
     */
    public static String getLatestMarketSummaryByLike(String symbolPattern) {
        String sql = "SELECT symbol, ask, bid FROM market_data "
                   + "WHERE UPPER(symbol) LIKE UPPER(?) "
                   + "ORDER BY id DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, symbolPattern + "%");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("symbol")
                     + " | bid: " + rs.getDouble("bid")
                     + " | ask: " + rs.getDouble("ask");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * *** METHOD BARU ***
     * Mengambil riwayat harga terbaru untuk suatu simbol.
     *
     * Data ini dikirim ke AI agar bisa mendeteksi tren sederhana:
     * apakah harga cenderung naik, turun, atau sideways dalam
     * beberapa tick terakhir yang tersimpan.
     *
     * @param symbol Nama simbol, e.g. "XAUUSDm" (case-insensitive)
     * @param limit  Jumlah data tick terakhir yang diambil (rekomendasi: 15–20)
     * @return String tabel harga siap pakai sebagai konteks AI,
     *         atau pesan informatif jika data belum tersedia
     */
    public static String getRecentPriceHistory(String symbol, int limit) {
        // Query diurutkan DESC (terbaru dulu) agar limit mengambil data paling relevan,
        // lalu kita balik di Java supaya AI membacanya dari lama ke baru (kronologis).
        String sql = "SELECT ask, bid, timestamp FROM market_data "
                   + "WHERE UPPER(symbol) = UPPER(?) "
                   + "ORDER BY id DESC LIMIT ?";

        StringBuilder sb = new StringBuilder();
        sb.append("Riwayat harga ").append(symbol)
          .append(" (").append(limit).append(" data terakhir, urutan: lama → baru):\n");

        // Tampung dulu ke array agar bisa dibalik urutannya
        java.util.List<String> rows = new java.util.ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, symbol);
            pstmt.setInt(2, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                rows.add(String.format("  [%s] Ask: %.2f | Bid: %.2f",
                    rs.getString("timestamp"),
                    rs.getDouble("ask"),
                    rs.getDouble("bid")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "Gagal mengambil riwayat harga: " + e.getMessage();
        }

        if (rows.isEmpty()) {
            return "Riwayat harga " + symbol + " belum tersedia. "
                 + "Pastikan EA MT5 sudah berjalan dan mengirim tick ke /api/market.";
        }

        // Balik urutan: sekarang dari lama ke baru
        java.util.Collections.reverse(rows);
        for (String row : rows) {
            sb.append(row).append("\n");
        }

        // Tambahkan ringkasan pergerakan untuk membantu AI
        // (harga pertama vs terakhir dalam snapshot ini)
        try {
            double firstAsk = Double.parseDouble(
                rows.get(0).split("Ask: ")[1].split(" \\|")[0].trim());
            double lastAsk  = Double.parseDouble(
                rows.get(rows.size() - 1).split("Ask: ")[1].split(" \\|")[0].trim());
            double delta    = lastAsk - firstAsk;
            String tren     = delta > 0 ? "↑ NAIK" : delta < 0 ? "↓ TURUN" : "→ SIDEWAYS";

            sb.append(String.format(
                "\nRingkasan snapshot: Harga awal %.2f → Harga akhir %.2f | Delta: %+.2f | Tren: %s\n",
                firstAsk, lastAsk, delta, tren));

        } catch (Exception ignored) {
            // Tidak kritis — lewati saja jika parse gagal
        }

        return sb.toString();
    }

    // =========================================================================
    // AKUN
    // =========================================================================

    /** @return Balance akun terbaru dari account_info, atau 0.0 */
    public static double getLatestBalance() {
        String sql = "SELECT balance FROM account_info ORDER BY last_update DESC LIMIT 1";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble("balance");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    /** @return Equity akun terbaru dari account_info, atau 0.0 */
    public static double getLatestEquity() {
        String sql = "SELECT equity FROM account_info ORDER BY last_update DESC LIMIT 1";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble("equity");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    /** @return Jumlah posisi yang masih OPEN */
    public static int getOpenTradesCount() {
        String sql = "SELECT COUNT(*) FROM trades WHERE status = 'OPEN'";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // =========================================================================
    // STATISTIK PERFORMA
    // =========================================================================

    /**
     * @return Win rate dalam persen (0–100), atau 0 jika belum ada trade CLOSED
     */
    public static double getWinRate() {
        String sql = "SELECT COUNT(*) as total, "
                   + "SUM(CASE WHEN profit_loss > 0 THEN 1 ELSE 0 END) as win "
                   + "FROM trades WHERE status = 'CLOSED'";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int total = rs.getInt("total");
                int win   = rs.getInt("win");
                return total == 0 ? 0 : (double) win / total * 100;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * @return Profit factor (gross profit / gross loss), atau 0 jika tidak ada loss
     */
    public static double getProfitFactor() {
        String sql = "SELECT "
                   + "SUM(CASE WHEN profit_loss > 0 THEN profit_loss ELSE 0 END) as grossProfit, "
                   + "SUM(CASE WHEN profit_loss < 0 THEN profit_loss ELSE 0 END) as grossLoss "
                   + "FROM trades WHERE status = 'CLOSED'";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                double grossProfit = rs.getDouble("grossProfit");
                double grossLoss   = Math.abs(rs.getDouble("grossLoss"));
                return grossLoss == 0 ? 0 : grossProfit / grossLoss;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * @return Maximum drawdown (nilai absolut dalam unit P&L akun)
     */
    public static double getMaxDrawdown() {
        String sql = "SELECT tanggal_buka, profit_loss FROM trades "
                   + "WHERE status = 'CLOSED' ORDER BY tanggal_buka";
        double cumulative = 0, peak = 0, maxDD = 0;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cumulative += rs.getDouble("profit_loss");
                if (cumulative > peak) peak = cumulative;
                double dd = peak - cumulative;
                if (dd > maxDD) maxDD = dd;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return maxDD;
    }

    /** @return Total jumlah trade yang sudah CLOSED */
    public static int getTotalTrades() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM trades WHERE status = 'CLOSED'")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Merangkum semua metrik akun menjadi satu blok teks.
     * Digunakan sebagai konteks oleh AI.
     *
     * @return String multi-baris berisi ringkasan performa
     */
    public static String getPerformanceSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Performa Akun Trading ===\n");
        sb.append(String.format("- Balance     : Rp %.2f\n", getLatestBalance()));
        sb.append(String.format("- Equity      : Rp %.2f\n", getLatestEquity()));
        sb.append(String.format("- Win Rate    : %.2f%%\n",  getWinRate()));
        sb.append(String.format("- Profit Factor: %.2f\n",   getProfitFactor()));
        sb.append(String.format("- Max Drawdown: Rp %.2f\n", getMaxDrawdown()));
        sb.append(String.format("- Total Closed: %d trade\n", getTotalTrades()));
        sb.append(String.format("- Open Now    : %d posisi\n", getOpenTradesCount()));
        return sb.toString();
    }

    /**
     * Mengambil N transaksi terakhir (OPEN maupun CLOSED) dalam format teks.
     * Digunakan sebagai konteks oleh AI.
     *
     * @param limit Jumlah transaksi yang diambil
     * @return String daftar transaksi
     */
    public static String getRecentTrades(int limit) {
        StringBuilder sb = new StringBuilder();
        sb.append(limit).append(" transaksi terakhir:\n");
        // Tidak menggunakan string concat untuk nilai limit karena ini adalah
        // integer literal dari kode kita sendiri, bukan input user. Aman.
        String sql = "SELECT id, tanggal_buka, aset, tipe, entry_price, "
                   + "exit_price, profit_loss, status "
                   + "FROM trades ORDER BY id DESC LIMIT " + limit;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int i = 1;
            while (rs.next()) {
                sb.append(i++).append(". ")
                  .append(rs.getString("tanggal_buka")).append(" | ")
                  .append(rs.getString("aset")).append(" | ")
                  .append(rs.getString("tipe"))
                  .append(" | Entry: ").append(rs.getDouble("entry_price"));
                if ("CLOSED".equals(rs.getString("status"))) {
                    sb.append(" | Exit: ").append(rs.getDouble("exit_price"));
                    sb.append(" | P/L: ").append(rs.getDouble("profit_loss"));
                } else {
                    sb.append(" | Status: OPEN");
                }
                sb.append("\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
