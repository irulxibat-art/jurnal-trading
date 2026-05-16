package com.irul.trading.util;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class AIVisionClient {
    private static final String API_KEY = "YOUR_GROQ_API_KEY";
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final OkHttpClient client;

    public AIVisionClient() {
        this.client = new OkHttpClient();
    }

    public static String encodeImageToBase64(File imageFile) throws IOException {
        return Base64.getEncoder().encodeToString(Files.readAllBytes(imageFile.toPath()));
    }

    public void askWithImage(String question, String base64Image, Callback callback) {
        String dataUri = "data:image/jpeg;base64," + base64Image;
        JSONObject body = new JSONObject();
        JSONArray messages = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        JSONArray content = new JSONArray();
        content.put(new JSONObject().put("type", "text").put("text", question));
        content.put(new JSONObject().put("type", "image_url").put("image_url", new JSONObject().put("url", dataUri)));
        userMsg.put("content", content);
        messages.put(userMsg);
        body.put("model", "meta-llama/llama-4-scout-17b-16e-instruct");
        body.put("messages", messages);
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
                try {
                    String resp = response.body().string();
                    JSONObject json = new JSONObject(resp);
                    String answer = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
                    callback.onSuccess(answer);
                } catch (Exception e) {
                    callback.onFailure("Parse error: " + e.getMessage());
                }
            }
        });
    }

    public interface Callback {
        void onSuccess(String result);
        void onFailure(String error);
    }
}