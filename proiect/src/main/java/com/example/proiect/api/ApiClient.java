package com.example.proiect.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ApiClient {

    public static class ApiResponse {
        public final int code;
        public final String body;

        public ApiResponse(int code, String body) {
            this.code = code;
            this.body = body;
        }

        public boolean isSuccess() {
            return code >= 200 && code < 300;
        }

        public JSONObject asJson() throws JSONException {
            return new JSONObject(body == null ? "{}" : body);
        }
    }

    private final String baseUrl;
    private final String token;

    public ApiClient(String baseUrl, String token) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        this.token = token;
    }

    public ApiResponse post(String path, JSONObject body) throws Exception {
        return request("POST", path, body);
    }

    public ApiResponse get(String path) throws Exception {
        return request("GET", path, null);
    }

    public ApiResponse patch(String path, JSONObject body) throws Exception {
        return request("PATCH", path, body);
    }

    public ApiResponse put(String path, JSONObject body) throws Exception {
        return request("PUT", path, body);
    }

    private ApiResponse request(String method, String path, JSONObject body) throws Exception {
        URL url = new URL(baseUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json");
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            try {
                conn.setRequestMethod(method);
            } catch (java.net.ProtocolException pe) {
                if ("PATCH".equals(method)) {
                    try {
                        java.lang.reflect.Field f = HttpURLConnection.class.getDeclaredField("method");
                        f.setAccessible(true);
                        f.set(conn, "PATCH");
                    } catch (Exception reflectFail) {
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
                    }
                } else {
                    throw pe;
                }
            }

            if (body != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload);
                }
            }

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            String responseBody = readStream(is);
            return new ApiResponse(code, responseBody);
        } finally {
            conn.disconnect();
        }
    }

    private static String readStream(InputStream is) throws Exception {
        if (is == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
