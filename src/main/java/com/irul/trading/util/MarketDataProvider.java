package com.irul.trading.util;

import org.json.JSONObject;

/**
 * Penyedia data pasar (market data) untuk digunakan oleh AI asisten trading.
 * Mengambil harga terbaru XAUUSDm dari database.
 */
public class MarketDataProvider {

    /**
     * Mendapatkan harga XAUUSDm terbaru dari tabel market_data.
     *
     * @return String berisi harga atau pesan bahwa data belum tersedia.
     */
    public static String getCurrentGoldPrice() {
        // Simbol yang digunakan oleh broker: XAUUSDm (dengan akhiran m)
        // Fungsi getLatestAsk sudah melakukan normalisasi huruf besar/kecil.
        double price = DatabaseHelper.getLatestAsk("XAUUSDm");
        String likeFallback = DatabaseHelper.getLatestMarketSummaryByLike("XAUUSD");
        // #region agent log
        DebugLogger.log("pre-fix", "H6", "MarketDataProvider.java:getCurrentGoldPrice",
            "AI market snapshot resolved", new JSONObject()
                .put("exactAsk", price)
                .put("likeFallback", likeFallback)
                .put("available", price > 0));
        // #endregion

        if (price > 0) {
            return String.format("Harga XAUUSDm saat ini adalah %.2f.", price);
        } else {
            return "Data market belum tersedia. Pastikan EA MT5 sudah berjalan dan mengirim data XAUUSDm ke server (port 8081).";
        }
    }
}