package com.example;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;

public class CrdpClient {

    private final String baseUrl;
    private final String policy;
    private final String token;
    private final String user;
    private SSLContext sslContext;

    private static final int TIMEOUT = 10; // 10 seconds

    /**
     * 생성자
     * 
     * @param endpoint CRDP 서버 주소 (예: "192.168.0.1:443")
     * @param policy   보호 정책 이름 (예: "P01")
     * @param token    JWT 인증 토큰
     * @param user     사용자 이름 (reveal 요청 시 필요)
     */
    public CrdpClient(String endpoint, String policy, String token, String user) {
        this.baseUrl = "https://" + endpoint;
        this.policy = policy;
        this.token = token;
        this.user = user;
        try {
            this.sslContext = getInsecureSslContext(); // 생성 시 한 번만 초기화
        } catch (Exception e) {
            throw new RuntimeException("SSLContext 초기화 실패", e);
        }
    }

    /**
     * 연결 워밍업 (Warm-up)
     */
    public void warmup() {
        try {
            String url = baseUrl + "/v1/protect";
            String json = String.format("{\"protection_policy_name\":\"%s\",\"data\":\"WARMUP\"}", policy);
            post(url, json);
        } catch (Exception e) {
            // 워밍업 실패는 무시
            System.err.println("CRDP Warmup failed (ignoring): " + e.getMessage());
        }
    }

    /**
     * 데이터 암호화 (Protect)
     */
    public String protect(String plaintext) throws Exception {
        if (plaintext == null)
            throw new IllegalArgumentException("입력 데이터는 null일 수 없습니다.");

        String url = baseUrl + "/v1/protect";
        String safeData = escapeJson(plaintext);
        String json = String.format("{\"protection_policy_name\":\"%s\",\"data\":\"%s\"}", policy, safeData);

        String response = post(url, json);
        return extractValue(response, "protected_data");
    }

    /**
     * 데이터 복호화 (Reveal)
     */
    public String reveal(String encrypted) throws Exception {
        if (encrypted == null)
            throw new IllegalArgumentException("입력 데이터는 null일 수 없습니다.");

        String url = baseUrl + "/v1/reveal";
        String safeData = escapeJson(encrypted);
        String json;
        if (user != null && !user.isEmpty()) {
            String safeUser = escapeJson(user);
            json = String.format("{\"protection_policy_name\":\"%s\",\"protected_data\":\"%s\",\"username\":\"%s\"}", policy, safeData, safeUser);
        } else {
            json = String.format("{\"protection_policy_name\":\"%s\",\"protected_data\":\"%s\"}", policy, safeData);
        }

        String response = post(url, json);
        return extractValue(response, "data");
    }

    private String post(String urlStr, String json) throws Exception {
        URL url = new URL(urlStr);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        conn.setSSLSocketFactory(this.sslContext.getSocketFactory());
        conn.setHostnameVerifier((hostname, session) -> true);

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        conn.setConnectTimeout(TIMEOUT * 1000);
        conn.setReadTimeout(TIMEOUT * 1000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        InputStream stream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();

        if (stream == null) {
            throw new RuntimeException("CRDP 서버 오류 (HTTP " + status + "): 응답 없음");
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null)
                response.append(line);
        }

        if (status >= 400) {
            throw new RuntimeException("CRDP 서버 오류 (HTTP " + status + "): " + response.toString());
        }

        return response.toString();
    }

    private String extractValue(String json, String key) {
        if (json == null)
            return null;

        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1)
            return null;

        int colonIndex = json.indexOf(":", keyIndex + searchKey.length());
        if (colonIndex == -1)
            return null;

        int valueStartIndex = json.indexOf("\"", colonIndex + 1);
        if (valueStartIndex == -1)
            return null;

        int valueEndIndex = valueStartIndex + 1;
        while (valueEndIndex < json.length()) {
            char c = json.charAt(valueEndIndex);
            if (c == '\\') {
                valueEndIndex += 2;
                continue;
            }
            if (c == '"') {
                break;
            }
            valueEndIndex++;
        }

        if (valueEndIndex >= json.length())
            return null;

        return json.substring(valueStartIndex + 1, valueEndIndex);
    }

    private String escapeJson(String data) {
        if (data == null)
            return "";
        return data.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private SSLContext getInsecureSslContext() throws Exception {
        TrustManager[] trustAll = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAll, new java.security.SecureRandom());
        return sc;
    }
}
