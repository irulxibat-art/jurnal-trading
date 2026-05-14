package com.irul.trading.util;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * NewsUtil — Mengambil berita finansial terkini dari RSS feed publik.
 *
 * Tidak memerlukan API key apapun. Semua sumber adalah RSS feed gratis
 * dari outlet berita finansial terpercaya.
 *
 * Feed yang digunakan:
 *  - FXStreet : berita forex, gold, dan analisis pasar
 *  - Investing.com Gold   : berita khusus XAU/USD
 *  - Investing.com USD    : berita Dollar Index (DXY)
 *  - Reuters Business     : berita ekonomi makro global
 *  - MarketWatch          : berita pasar saham & komoditas
 *
 * Arsitektur:
 *  - Setiap feed di-fetch dengan timeout singkat (5 detik connect, 8 detik read).
 *  - Jika satu feed gagal (down/timeout), feed lainnya tetap berjalan.
 *  - Teks HTML di-strip sebelum dikirim ke AI.
 *  - Hasil dibatasi per-feed untuk menghindari context window yang terlalu besar.
 */
public class NewsUtil {

    // -------------------------------------------------------------------------
    // Daftar RSS Feed
    // Struktur: { "Nama Sumber", "URL Feed", "Kata kunci filter (null = ambil semua)" }
    // -------------------------------------------------------------------------
    private static final String[][] FEEDS = {
        // Forex & Gold — sangat relevan untuk XAUUSDm
        { "FXStreet",          "https://www.fxstreet.com/rss/news",                    null },
        // Gold specific
        { "Investing Gold",    "https://www.investing.com/rss/news_25.rss",             null },
        // DXY / Dollar Index
        { "Investing USD",     "https://www.investing.com/rss/news_301.rss",            null },
        // Ekonomi makro
        { "Reuters Business",  "https://feeds.reuters.com/reuters/businessNews",        null },
        // Pasar global
        { "MarketWatch",       "https://feeds.marketwatch.com/marketwatch/topstories/", null },
    };

    // Batas berita per feed (agar context AI tidak overload)
    private static final int MAX_PER_FEED = 3;

    // Batas panjang deskripsi per berita (karakter)
    private static final int MAX_DESC_LEN = 200;

    // Timeout koneksi & baca (ms)
    private static final int CONNECT_TIMEOUT = 5_000;
    private static final int READ_TIMEOUT    = 8_000;

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Model data satu item berita.
     */
    public static class NewsItem {
        public final String source;
        public final String title;
        public final String description;
        public final String pubDate;

        public NewsItem(String source, String title, String description, String pubDate) {
            this.source      = source;
            this.title       = title;
            this.description = description;
            this.pubDate     = pubDate;
        }
    }

    /**
     * Mengambil berita dari semua feed yang relevan dengan keyword tertentu.
     *
     * @param keyword Kata kunci filter (null/empty = ambil semua berita terbaru)
     * @return Daftar berita yang sudah difilter, diurutkan sesuai urutan feed
     */
    public static List<NewsItem> fetchNews(String keyword) {
        List<NewsItem> results = new ArrayList<>();
        for (String[] feed : FEEDS) {
            try {
                List<NewsItem> fromFeed = fetchFromFeed(feed[0], feed[1], keyword, MAX_PER_FEED);
                results.addAll(fromFeed);
            } catch (Exception e) {
                // Satu feed gagal tidak menghentikan feed lainnya
                System.err.println("[NewsUtil] Gagal fetch dari " + feed[0] + ": " + e.getMessage());
            }
        }
        return results;
    }

    /**
     * Membangun blok teks konteks berita siap pakai untuk sistem prompt AI.
     * Jika tidak ada berita yang berhasil diambil, mengembalikan pesan informatif.
     *
     * @param userMessage Pesan user asli — digunakan untuk ekstrak keyword otomatis
     * @return String multi-baris berisi daftar berita + metadata
     */
    public static String buildNewsContext(String userMessage) {
        String keyword = extractKeyword(userMessage);

        System.out.println("[NewsUtil] Fetching news for keyword: '" + keyword + "'");
        List<NewsItem> news = fetchNews(keyword);

        if (news.isEmpty()) {
            return "Berita finansial terkini tidak dapat diambil saat ini "
                 + "(periksa koneksi internet). AI akan menjawab berdasarkan pengetahuan umum.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("════════════════════════════════════\n");
        sb.append("BERITA TERKINI — ").append(keyword.toUpperCase()).append("\n");
        sb.append("(Sumber: RSS feed publik, ").append(news.size()).append(" artikel)\n");
        sb.append("════════════════════════════════════\n");

        for (int i = 0; i < news.size(); i++) {
            NewsItem item = news.get(i);
            sb.append(i + 1).append(". [").append(item.source).append("]\n");
            sb.append("   Judul   : ").append(item.title).append("\n");
            if (!item.description.isEmpty()) {
                String desc = item.description.length() > MAX_DESC_LEN
                    ? item.description.substring(0, MAX_DESC_LEN) + "..."
                    : item.description;
                sb.append("   Ringkasan: ").append(desc).append("\n");
            }
            if (!item.pubDate.isEmpty()) {
                sb.append("   Tanggal : ").append(item.pubDate).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // =========================================================================
    // Private — Fetch & Parse RSS
    // =========================================================================

    /**
     * Mengambil dan mem-parse satu RSS feed.
     *
     * @param sourceName Nama sumber untuk label pada hasil
     * @param feedUrl    URL feed RSS
     * @param keyword    Filter kata kunci (null = ambil semua)
     * @param max        Jumlah maksimal item yang diambil dari feed ini
     * @return Daftar NewsItem dari feed tersebut
     */
    private static List<NewsItem> fetchFromFeed(
            String sourceName, String feedUrl, String keyword, int max) throws Exception {

        List<NewsItem> items = new ArrayList<>();

        // Buka koneksi HTTP
        URL url = new URL(feedUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        conn.setRequestProperty("Accept",
            "application/rss+xml, application/xml, text/xml");
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setInstanceFollowRedirects(true);

        int status = conn.getResponseCode();
        if (status != 200) {
            throw new Exception("HTTP " + status);
        }

        // Parse XML RSS
        try (InputStream is = conn.getInputStream()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable external entity processing (security)
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            // Suppress console warnings dari parser
            builder.setErrorHandler(null);
            Document doc = builder.parse(is);

            NodeList itemNodes = doc.getElementsByTagName("item");
            int count = 0;

            for (int i = 0; i < itemNodes.getLength() && count < max; i++) {
                Node node = itemNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) continue;

                Element el    = (Element) node;
                String title  = getTagText(el, "title");
                String desc   = cleanHtml(getTagText(el, "description"));
                String date   = getTagText(el, "pubDate");

                // Skip item yang tidak mengandung keyword (jika ada filter)
                if (keyword != null && !keyword.isEmpty()) {
                    String combined = (title + " " + desc).toLowerCase();
                    if (!combined.contains(keyword.toLowerCase())) continue;
                }

                // Skip item dengan judul kosong
                if (title.isEmpty()) continue;

                items.add(new NewsItem(sourceName, title, desc, date));
                count++;
            }
        }

        conn.disconnect();
        return items;
    }

    // =========================================================================
    // Private — Helpers
    // =========================================================================

    /**
     * Membaca teks dari tag XML pertama yang ditemukan di dalam element.
     * Menangani CDATA section secara otomatis.
     */
    private static String getTagText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) return "";

        Node first = nodes.item(0);
        if (first == null) return "";

        // Kumpulkan semua child text nodes (termasuk CDATA)
        StringBuilder sb = new StringBuilder();
        NodeList children = first.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE
             || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                sb.append(child.getNodeValue());
            }
        }
        return sb.toString().trim();
    }

    /**
     * Menghapus tag HTML dan HTML entities dari string deskripsi RSS.
     * RSS feed sering menyertakan HTML di field description.
     */
    private static String cleanHtml(String text) {
        if (text == null || text.isEmpty()) return "";
        return text
            .replaceAll("<[^>]+>", "")       // hapus tag HTML
            .replaceAll("&nbsp;",   " ")
            .replaceAll("&amp;",    "&")
            .replaceAll("&lt;",     "<")
            .replaceAll("&gt;",     ">")
            .replaceAll("&quot;",   "\"")
            .replaceAll("&#\\d+;",  "")      // hapus numeric entities
            .replaceAll("\\s+",     " ")      // normalkan whitespace
            .trim();
    }

    /**
     * Mengekstrak kata kunci pencarian dari pesan user.
     *
     * Pemetaan:
     *  - Pesan menyebut gold/emas/xauusd → keyword "gold"
     *  - Pesan menyebut dollar/usd/dxy   → keyword "dollar"
     *  - Pesan menyebut bitcoin/btc       → keyword "bitcoin"
     *  - Pesan menyebut fomc/fed          → keyword "federal reserve"
     *  - Pesan menyebut cpi/inflasi       → keyword "inflation"
     *  - Pesan menyebut nfp/nonfarm       → keyword "nonfarm payroll"
     *  - Default                          → keyword "forex market" (umum)
     *
     * @param userMessage Pesan asli dari user
     * @return Keyword yang akan digunakan untuk filter RSS
     */
    public static String extractKeyword(String userMessage) {
        if (userMessage == null) return "forex market";
        String msg = userMessage.toLowerCase();

        if (msg.contains("gold")   || msg.contains("emas")    || msg.contains("xauusd")) return "gold";
        if (msg.contains("dollar") || msg.contains("usd")     || msg.contains("dxy"))    return "dollar";
        if (msg.contains("bitcoin")|| msg.contains("btc")     || msg.contains("crypto")) return "bitcoin";
        if (msg.contains("fomc")   || msg.contains("federal reserve") || msg.contains("fed rate")) return "federal reserve";
        if (msg.contains("cpi")    || msg.contains("inflasi") || msg.contains("inflation"))         return "inflation";
        if (msg.contains("nfp")    || msg.contains("nonfarm") || msg.contains("payroll"))           return "nonfarm payroll";
        if (msg.contains("oil")    || msg.contains("minyak")  || msg.contains("crude"))             return "oil crude";
        if (msg.contains("silver") || msg.contains("perak")   || msg.contains("xagusd"))            return "silver";
        if (msg.contains("euro")   || msg.contains("eur"))                                          return "euro";
        if (msg.contains("china")  || msg.contains("cina")    || msg.contains("yuan"))              return "china economy";
        if (msg.contains("berita") || msg.contains("news")    || msg.contains("update"))            return "forex market";

        return "forex market"; // default fallback
    }
}