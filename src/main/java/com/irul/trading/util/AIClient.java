package com.irul.trading.util;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;

public class AIClient {
    private static final String API_KEY = "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"; // ganti
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final OkHttpClient client;

    public AIClient() {
        this.client = new OkHttpClient();
    }

    public void askWithContext(String userMsg, Callback callback) {
        String perf = DatabaseHelper.getPerformanceSummary();
        String trades = DatabaseHelper.getRecentTrades(5);
        String marketSnapshot = MarketDataProvider.getCurrentGoldPrice();
        // #region agent log
        DebugLogger.log("post-fix", "H13", "AIClient.java:21", "askWithContext using market snapshot",
                new JSONObject().put("marketSnapshot", marketSnapshot));
        // #endregion
        String prompt = "Kamu adalah asisten trading bernama Nara. Data akun dan performa:\n" + perf +
                "\n\n5 transaksi terakhir:\n" + trades +
                "\n\nData market terbaru dari MT5:\n" + marketSnapshot +
                "\n\nJawab pertanyaan: " + userMsg;
        askRaw(prompt, callback);
    }

    public void askWithFunctionCalling(String userMsg, Callback callback) {
        try {
            // #region agent log
            DebugLogger.log("pre-fix", "H7", "AIClient.java:28", "askWithFunctionCalling entry",
                    new JSONObject().put("userMsg", userMsg));
            // #endregion
            String marketSnapshot = MarketDataProvider.getCurrentGoldPrice();
            // #region agent log
            DebugLogger.log("post-fix", "H10", "AIClient.java:35", "Using deterministic market snapshot",
                    new JSONObject().put("marketSnapshot", marketSnapshot));
            // #endregion
            if (containsMarketIntent(userMsg)) {
                // #region agent log
                DebugLogger.log("post-fix", "H11", "AIClient.java:40", "Returning direct market snapshot response",
                        new JSONObject().put("marketSnapshot", marketSnapshot));
                // #endregion
                callback.onSuccess(marketSnapshot);
                return;
            }

            JSONObject body = new JSONObject();
            body.put("model", "llama-3.1-8b-instant");
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "Gunakan data pasar berikut sebagai sumber kebenaran utama: " + marketSnapshot));
            messages.put(new JSONObject().put("role", "user").put("content", userMsg));
            body.put("messages", messages);
            body.put("temperature", 0.7);
            body.put("max_tokens", 1024);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + API_KEY)
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onFailure("HTTP " + response.code());
                        return;
                    }
                    String resp = response.body().string();
                    JSONObject json = new JSONObject(resp);
                    JSONObject choice = json.getJSONArray("choices").getJSONObject(0);
                    JSONObject msg = choice.getJSONObject("message");
                    // #region agent log
                    DebugLogger.log("post-fix", "H10", "AIClient.java:70", "AI response generated with market snapshot",
                            new JSONObject().put("content", msg.optString("content", "")));
                    // #endregion
                    callback.onSuccess(msg.optString("content", ""));
                }
            });
        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }

    private boolean containsMarketIntent(String userMsg) {
        if (userMsg == null) return false;
        String lower = userMsg.toLowerCase();
        return lower.contains("harga emas")
                || lower.contains("xauusd")
                || lower.contains("gold")
                || lower.contains("harga market")
                || lower.contains("harga pasar");
    }

    private void sendFunctionResult(String toolCallId, String result, Callback callback) {
        try {
            JSONObject toolMsg = new JSONObject();
            toolMsg.put("role", "tool");
            toolMsg.put("tool_call_id", toolCallId);
            toolMsg.put("content", result);
            // #region agent log
            DebugLogger.log("pre-fix", "H6", "AIClient.java:99", "Sending tool result to second completion",
                    new JSONObject().put("toolCallId", toolCallId).put("result", result));
            // #endregion

            JSONObject body = new JSONObject();
            body.put("model", "llama-3.1-8b-instant");
            body.put("messages", new JSONArray().put(toolMsg));
            body.put("temperature", 0.7);
            body.put("max_tokens", 1024);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + API_KEY)
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e.getMessage());
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        // #region agent log
                        DebugLogger.log("pre-fix", "H6", "AIClient.java:124", "Second completion failed",
                                new JSONObject().put("httpCode", response.code()));
                        // #endregion
                        callback.onFailure("HTTP " + response.code());
                        return;
                    }
                    try {
                        String resp = response.body().string();
                        JSONObject json = new JSONObject(resp);
                        String answer = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                        // #region agent log
                        DebugLogger.log("pre-fix", "H6", "AIClient.java:134", "Second completion succeeded",
                                new JSONObject().put("answer", answer));
                        // #endregion
                        callback.onSuccess(answer);
                    } catch (Exception e) {
                        callback.onFailure("Parse error: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }

    private void askRaw(String prompt, Callback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", "llama-3.1-8b-instant");
            body.put("messages", new JSONArray().put(new JSONObject().put("role", "user").put("content", prompt)));
            body.put("temperature", 0.7);
            body.put("max_tokens", 1024);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + API_KEY)
                    .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e.getMessage());
                }
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onFailure("HTTP " + response.code());
                        return;
                    }
                    String resp = response.body().string();
                    JSONObject json = new JSONObject(resp);
                    String answer = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                    callback.onSuccess(answer);
                }
            });
        } catch (Exception e) {
            callback.onFailure(e.getMessage());
        }
    }

    public interface Callback {
        void onSuccess(String result);
        void onFailure(String error);
    }
}