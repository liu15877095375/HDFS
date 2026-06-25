package com.cgrs.driver.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AiClientUtil {

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.api.key}")
    private String apiKey;

    public String analyze(String content, String prompt) {
        String jsonBody = buildRequestBody(content, prompt);
        
        System.out.println("=== AI Request ===");
        System.out.println("URL: " + apiUrl);
        System.out.println("Content Length: " + jsonBody.length() + " characters");
        System.out.println("Content Preview: " + (jsonBody.length() > 200 ? jsonBody.substring(0, 200) + "..." : jsonBody));

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("User-Agent", "HDFS-Cloud-Drive/1.0.0");
            connection.setRequestProperty("Accept", "application/json");
            
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(300000);
            connection.setDoOutput(true);
            connection.setDoInput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            BufferedReader br;
            if (responseCode >= 200 && responseCode < 300) {
                br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            } else {
                br = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            System.out.println("Response Body: " + (response.length() > 500 ? response.substring(0, 500) + "..." : response));

            if (responseCode >= 200 && responseCode < 300) {
                return parseResponse(response.toString());
            } else {
                throw new RuntimeException("AI API Error " + responseCode + ": " + response);
            }

        } catch (ResourceAccessException e) {
            throw new RuntimeException("网络连接失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("AI请求失败: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(String content, String prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", "deepseek-chat");
        body.put("temperature", 0.7);
        body.put("max_tokens", 2048);

        Map<String, String> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一个专业的文件分析助手。");

        Map<String, String> userMsg = new LinkedHashMap<>();
        String userContent = "请分析以下文件内容：\n\n" + content + "\n\n分析要求：" + prompt;
        userMsg.put("role", "user");
        userMsg.put("content", userContent);

        body.put("messages", List.of(systemMsg, userMsg));

        return mapToJson(body);
    }

    private String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof List) {
                sb.append(listToJson((List<?>) value));
            } else if (value instanceof Map) {
                sb.append(mapToJson((Map<String, Object>) value));
            } else {
                sb.append("null");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(",");
            first = false;
            if (item instanceof String) {
                sb.append("\"").append(escapeJson((String) item)).append("\"");
            } else if (item instanceof Map) {
                sb.append(mapToJson((Map<String, Object>) item));
            } else {
                sb.append("null");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            switch (c) {
                case '\"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private String parseResponse(String json) {
        try {
            json = json.trim();
            if (!json.startsWith("{") || !json.endsWith("}")) {
                throw new RuntimeException("Invalid JSON response");
            }

            int choicesIndex = json.indexOf("\"choices\"");
            if (choicesIndex == -1) {
                throw new RuntimeException("No choices in response");
            }

            int bracketStart = json.indexOf("[", choicesIndex);
            int bracketEnd = findMatchingBracket(json, bracketStart);
            if (bracketStart == -1 || bracketEnd == -1) {
                throw new RuntimeException("Invalid choices array");
            }

            String choicesStr = json.substring(bracketStart, bracketEnd + 1);
            
            int messageIndex = choicesStr.indexOf("\"message\"");
            if (messageIndex == -1) {
                throw new RuntimeException("No message in choice");
            }

            int colonIndex = choicesStr.indexOf(":", messageIndex);
            int contentIndex = choicesStr.indexOf("\"content\"", colonIndex);
            if (contentIndex == -1) {
                throw new RuntimeException("No content in message");
            }

            int contentColon = choicesStr.indexOf(":", contentIndex);
            int contentStart = choicesStr.indexOf("\"", contentColon + 1);
            int contentEnd = findMatchingQuote(choicesStr, contentStart + 1);
            if (contentStart == -1 || contentEnd == -1) {
                throw new RuntimeException("Invalid content");
            }

            return unescapeJson(choicesStr.substring(contentStart + 1, contentEnd));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response: " + e.getMessage() + "\nResponse: " + (json.length() > 500 ? json.substring(0, 500) + "..." : json), e);
        }
    }

    private int findMatchingBracket(String str, int start) {
        int depth = 1;
        for (int i = start + 1; i < str.length(); i++) {
            if (str.charAt(i) == '[') depth++;
            else if (str.charAt(i) == ']') depth--;
            if (depth == 0) return i;
        }
        return -1;
    }

    private int findMatchingQuote(String str, int start) {
        boolean escaped = false;
        for (int i = start; i < str.length(); i++) {
            if (escaped) {
                escaped = false;
                continue;
            }
            if (str.charAt(i) == '\\') {
                escaped = true;
            } else if (str.charAt(i) == '\"') {
                return i;
            }
        }
        return -1;
    }

    private String unescapeJson(String str) {
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case '\"': sb.append('\"'); break;
                    case '\\': sb.append('\\'); break;
                    case 'u':
                        if (i + 4 < str.length()) {
                            String hex = str.substring(i + 1, i + 5);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        } else {
                            sb.append(c);
                        }
                        break;
                    default: sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}