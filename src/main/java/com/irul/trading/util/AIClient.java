package com.irul.trading.util;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * AIClient — jembatan antara aplikasi trading dan model bahasa (Groq LLM).
 *
 * Perubahan dari versi sebelumnya:
 *  + Integrasi NewsUtil: AI sekarang dapat mengambil dan merangkum berita
 *    finansial terkini dari RSS feed publik sebelum menjawab pertanyaan.
 *  + Context building (DB queries + news fetch) dipindahkan ke background thread
 *    menggunakan ExecutorService, sehingga EDT (UI thread) tidak pernah diblokir.
 *  + Tiga jalur respons: analysis (harga+news+DB), news-only, general (DB saja).
 *
 * Catatan keamanan:
 *  API key sebaiknya dibaca dari System Property / environment variable,
 *  bukan di-hardcode. Di produksi, gunakan file konfigurasi eksternal.
 */
public class AIClient {

    // -------------------------------------------------------------------------
    // Konfigurasi
    // -------------------------------------------------------------------------

    private static final String API_KEY = System.getProperty(
        "GROQ_API_KEY",
        "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"
    );
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    /**
     * Model analisis: lebih powerful, digunakan ketika butuh reasoning mendalam.
     * Fallback ke model cepat jika butuh respons instan.
     */
    private static final String MODEL_ANALYSIS = "llama-3.3-70b-versatile";
    private static final String MODEL_FAST     = "llama-3.1-8b-instant";

    // Konstanta konteks
    private static final int PRICE_HISTORY_LIMIT = 20;
    private static final int RECENT_TRADES_LIMIT  = 5;

    private final OkHttpClient     httpClient;

    /**
     * Thread pool untuk context building (DB + news fetch).
     * Menggunakan single thread agar request tidak saling tumpang tindih.
     * Daemon thread: otomatis mati saat aplikasi ditutup.
     */
    private final ExecutorService  bgExecutor;

    // =========================================================================
    // Constructor
    // =========================================================================

    public AIClient() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();

        this.bgExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ai-context-builder");
            t.setDaemon(true);
            return t;
        });
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Memproses pertanyaan yang berkaitan dengan analisis pasar.
     *
     * Alur di background thread:
     *  1. Ambil data pasar terkini (harga, histori) dari DB
     *  2. Ambil statistik akun dan transaksi dari DB
     *  3. Jika pertanyaan berkaitan dengan berita → fetch RSS news
     *  4. Rakit semua menjadi system prompt
     *  5. Kirim ke Groq API (async)
     *
     * Semua langkah 1–4 berjalan di bgExecutor (bukan EDT),
     * sehingga UI tetap responsif selama proses berlangsung.
     *
     * @param userMsg  Pertanyaan dari user
     * @param callback Dipanggil di OkHttp thread setelah AI merespons
     */
    public void askWithFunctionCalling(String userMsg, Callback callback) {
        DebugLogger.log("post-fix", "H7", "AIClient.java",
            "askWithFunctionCalling entry", new JSONObject().put("userMsg", userMsg));

        bgExecutor.submit(() -> {
            try {
                // --- 1. Data pasar dari database ---
                String marketSnapshot = MarketDataProvider.getCurrentGoldPrice();
                String priceHistory   = DatabaseHelper.getRecentPriceHistory("XAUUSDm", PRICE_HISTORY_LIMIT);
                String perfSummary    = DatabaseHelper.getPerformanceSummary();
                String recentTrades   = DatabaseHelper.getRecentTrades(RECENT_TRADES_LIMIT);

                // --- 2. Berita (jika relevan) ---
                String newsContext;
                if (isNewsIntent(userMsg)) {
                    System.out.println("[AIClient] News intent detected, fetching RSS...");
                    newsContext = NewsUtil.buildNewsContext(userMsg);
                    DebugLogger.log("post-fix", "H12", "AIClient.java",
                        "News context fetched", new JSONObject()
                            .put("keyword", NewsUtil.extractKeyword(userMsg))
                            .put("newsLength", newsContext.length()));
                } else {
                    newsContext = "";
                }

                DebugLogger.log("post-fix", "H10", "AIClient.java",
                    "Full context assembled", new JSONObject()
                        .put("hasMarket", !marketSnapshot.contains("belum tersedia"))
                        .put("hasNews", !newsContext.isEmpty())
                        .put("perfLength", perfSummary.length()));

                // --- 3. Rakit system prompt ---
                String systemPrompt = buildAnalystSystemPrompt(
                    marketSnapshot, priceHistory, perfSummary, recentTrades, newsContext);

                // --- 4. Kirim ke Groq ---
                sendToGroq(systemPrompt, userMsg, MODEL_ANALYSIS, 0.4, 1800, callback);

            } catch (Exception e) {
                callback.onFailure("Error membangun konteks analisis: " + e.getMessage());
            }
        });
    }

    /**
     * Memproses pertanyaan umum (tidak butuh data pasar real-time).
     * Tetap menyertakan performa akun sebagai konteks dasar.
     *
     * @param userMsg  Pertanyaan dari user
     * @param callback Callback hasil
     */
    public void askWithContext(String userMsg, Callback callback) {
        bgExecutor.submit(() -> {
            try {
                String perf   = DatabaseHelper.getPerformanceSummary();
                String trades = DatabaseHelper.getRecentTrades(RECENT_TRADES_LIMIT);
                String market = MarketDataProvider.getCurrentGoldPrice();

                String prompt = "Kamu adalah Nara, asisten trading profesional.\n\n"
                    + "DATA AKUN:\n" + perf
                    + "\nTRANSAKSI TERAKHIR:\n" + trades
                    + "\nDATA PASAR TERKINI:\n" + market
                    + "\n\nJawab dalam Bahasa Indonesia yang jelas dan profesional.\n"
                    + "Pertanyaan: " + userMsg;

                askRaw(prompt, MODEL_FAST, callback);

            } catch (Exception e) {
                callback.onFailure("Error membangun konteks: " + e.getMessage());
            }
        });
    }

    // =========================================================================
    // PRIVATE — Intent Detection
    // =========================================================================

    /**
     * Mendeteksi apakah pertanyaan user membutuhkan data berita terkini.
     *
     * Pertanyaan yang memicu news fetch:
     *  - Kata "berita", "news", "update", "terbaru"
     *  - Nama event ekonomi: "fomc", "cpi", "nfp", "fed", "payroll"
     *  - Topik makro: "inflasi", "suku bunga", "interest rate"
     *  - Instrumen berita-sensitif yang disebut eksplisit: "dxy", "dollar"
     *  - Pertanyaan yang mengandung "kenapa naik/turun" (butuh fundamental)
     */
    private boolean isNewsIntent(String userMsg) {
        if (userMsg == null) return false;
        String msg = userMsg.toLowerCase();

        // Kata kunci berita langsung
        if (msg.contains("berita")  || msg.contains("news")      || msg.contains("update")
         || msg.contains("terbaru") || msg.contains("latest")    || msg.contains("hari ini")) {
            return true;
        }
        // Event ekonomi kalender
        if (msg.contains("fomc")    || msg.contains("federal reserve") || msg.contains("fed rate")
         || msg.contains("cpi")     || msg.contains("inflasi")   || msg.contains("inflation")
         || msg.contains("nfp")     || msg.contains("nonfarm")   || msg.contains("payroll")
         || msg.contains("gdp")     || msg.contains("pdb")       || msg.contains("suku bunga")
         || msg.contains("interest rate")                         || msg.contains("rate hike")) {
            return true;
        }
        // Pertanyaan sebab-akibat (butuh fundamental/berita)
        if (msg.contains("kenapa")  || msg.contains("mengapa")   || msg.contains("why")
         || msg.contains("penyebab")|| msg.contains("faktor")    || msg.contains("fundamental")) {
            return true;
        }
        // Topik makro geopolitik
        if (msg.contains("geopolitik")|| msg.contains("perang")  || msg.contains("war")
         || msg.contains("sanction") || msg.contains("sanksi")   || msg.contains("trump")
         || msg.contains("china")   || msg.contains("opec")      || msg.contains("recession")) {
            return true;
        }
        return false;
    }

    // =========================================================================
    // PRIVATE — System Prompt Builder
    // =========================================================================

    /**
     * Merakit system prompt lengkap untuk sesi analisis.
     *
     * Section newsContext bersifat opsional: jika string kosong,
     * section tersebut tidak ditambahkan ke prompt (hemat token).
     *
     * @param marketSnapshot Harga terkini dari MT5
     * @param priceHistory   Riwayat tick untuk analisis tren
     * @param perfSummary    Statistik performa akun
     * @param recentTrades   N transaksi terakhir
     * @param newsContext    Berita terkini (bisa kosong)
     * @return System prompt siap kirim ke Groq
     */
    private String buildAnalystSystemPrompt(
            String marketSnapshot,
            String priceHistory,
            String perfSummary,
            String recentTrades,
            String newsContext) {

        StringBuilder sb = new StringBuilder();

        sb.append("Kamu adalah NARA, analis dan asisten trading profesional yang berpengalaman.\n\n");

        sb.append("PERANMU:\n");
        sb.append("Bantu trader memahami kondisi pasar, berita fundamental, dan performa akun\n");
        sb.append("dengan analisis yang terstruktur, objektif, dan actionable.\n\n");

        sb.append("KEMAMPUANMU:\n");
        sb.append("1. Analisis tren harga dari riwayat tick (naik/turun/sideways).\n");
        sb.append("2. Identifikasi support/resistance dari data harga tersedia.\n");
        sb.append("3. Rangkum dan interpretasi berita fundamental (jika tersedia).\n");
        sb.append("4. Hubungkan berita dengan pergerakan harga (news-price correlation).\n");
        sb.append("5. Evaluasi performa akun dan beri saran manajemen risiko.\n");
        sb.append("6. Skenario bullish/bearish/netral berdasarkan data yang ada.\n\n");

        sb.append("ATURAN WAJIB:\n");
        sb.append("- JANGAN mengarang data harga atau berita. Gunakan HANYA data di bawah.\n");
        sb.append("- Jika data tidak cukup, katakan jujur.\n");
        sb.append("- Setiap saran entry/exit WAJIB disertai disclaimer risiko singkat.\n");
        sb.append("- Jawab dalam Bahasa Indonesia yang profesional dan mudah dipahami.\n");
        sb.append("- Gunakan format terstruktur (poin-poin) untuk jawaban panjang.\n\n");

        // --- Data Pasar ---
        sb.append("════════════════════════════════════\n");
        sb.append("DATA PASAR TERKINI (sumber: EA MT5)\n");
        sb.append("════════════════════════════════════\n");
        sb.append(marketSnapshot).append("\n\n");

        // --- Riwayat Harga ---
        sb.append("════════════════════════════════════\n");
        sb.append("RIWAYAT HARGA (analisis tren)\n");
        sb.append("════════════════════════════════════\n");
        sb.append(priceHistory).append("\n\n");

        // --- Berita (hanya jika ada) ---
        if (!newsContext.isEmpty()) {
            sb.append(newsContext).append("\n");
        }

        // --- Performa Akun ---
        sb.append("════════════════════════════════════\n");
        sb.append("PERFORMA AKUN TRADING\n");
        sb.append("════════════════════════════════════\n");
        sb.append(perfSummary).append("\n\n");

        // --- Transaksi Terakhir ---
        sb.append("════════════════════════════════════\n");
        sb.append("TRANSAKSI TERAKHIR\n");
        sb.append("════════════════════════════════════\n");
        sb.append(recentTrades).append("\n");

        return sb.toString();
    }

    // =========================================================================
    // PRIVATE — HTTP Layer
    // =========================================================================

    /**
     * Mengirim request ke Groq dengan system + user message terpisah.
     * Menggunakan OkHttp enqueue sehingga non-blocking.
     *
     * @param systemPrompt Data konteks lengkap untuk AI
     * @param userMsg      Pertanyaan user yang bersih
     * @param model        ID model Groq
     * @param temperature  Kreativitas respons (0.0–1.0)
     * @param maxTokens    Batas panjang respons
     * @param callback     Dipanggil setelah AI merespons
     */
    private void sendToGroq(
            String systemPrompt, String userMsg,
            String model, double temperature, int maxTokens,
            Callback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("model",       model);
            body.put("temperature", temperature);
            body.put("max_tokens",  maxTokens);

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                .put("role",    "system")
                .put("content", systemPrompt));
            messages.put(new JSONObject()
                .put("role",    "user")
                .put("content", userMsg));
            body.put("messages", messages);

            Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type",   "application/json")
                .post(RequestBody.create(body.toString(),
                    MediaType.parse("application/json")))
                .build();

            httpClient.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure("Koneksi ke Groq gagal: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful()) {
                            String err = responseBody != null ? responseBody.string() : "(no body)";
                            callback.onFailure("Groq HTTP " + response.code() + ": " + err);
                            return;
                        }
                        if (responseBody == null) {
                            callback.onFailure("Response body kosong.");
                            return;
                        }
                        try {
                            String raw    = responseBody.string();
                            String answer = new JSONObject(raw)
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                            DebugLogger.log("post-fix", "H10", "AIClient.java",
                                "AI response received", new JSONObject()
                                    .put("model",  model)
                                    .put("length", answer.length()));

                            callback.onSuccess(answer);
                        } catch (Exception e) {
                            callback.onFailure("Parse response gagal: " + e.getMessage());
                        }
                    }
                }
            });

        } catch (Exception e) {
            callback.onFailure("Gagal membangun HTTP request: " + e.getMessage());
        }
    }

    /**
     * Mengirim satu prompt tunggal (tanpa system prompt terpisah).
     * Digunakan oleh askWithContext() untuk pertanyaan umum.
     */
    private void askRaw(String prompt, String model, Callback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("model",       model);
            body.put("temperature", 0.7);
            body.put("max_tokens",  1024);
            body.put("messages", new JSONArray()
                .put(new JSONObject()
                    .put("role",    "user")
                    .put("content", prompt)));

            Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type",   "application/json")
                .post(RequestBody.create(body.toString(),
                    MediaType.parse("application/json")))
                .build();

            httpClient.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure("Koneksi gagal: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful()) {
                            callback.onFailure("HTTP " + response.code());
                            return;
                        }
                        if (responseBody == null) {
                            callback.onFailure("Response body kosong.");
                            return;
                        }
                        String raw    = responseBody.string();
                        String answer = new JSONObject(raw)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                        callback.onSuccess(answer);
                    } catch (Exception e) {
                        callback.onFailure("Parse error: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            callback.onFailure("Error: " + e.getMessage());
        }
    }

    // =========================================================================
    // Callback Interface
    // =========================================================================

    public interface Callback {
        void onSuccess(String result);
        void onFailure(String error);
    }
}