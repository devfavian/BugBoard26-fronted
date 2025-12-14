package application;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthApi {

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException() { super("401 Unauthorized"); }
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static LoginResponse login(String baseUrl, String email, String psw) throws Exception {
        String url = baseUrl + "/bugboard/users/login";

        // JSON coerente con LoginRequest del backend: {email, psw}
        String json = "{"
                + "\"email\":\"" + escapeJson(email) + "\","
                + "\"psw\":\"" + escapeJson(psw) + "\""
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (resp.statusCode() == 200) {
            return parseLoginResponse(resp.body());
        }
        if (resp.statusCode() == 401) {
            throw new UnauthorizedException();
        }

        throw new RuntimeException("HTTP " + resp.statusCode() + " - " + resp.body());
    }

    // Parse minimale: si aspetta {"userID":5,"role":"ADMIN"}
    private static LoginResponse parseLoginResponse(String body) {
        Long userId = extractLong(body, "\"userID\"\\s*:\\s*(\\d+)");
        String role = extractString(body, "\"role\"\\s*:\\s*\"([^\"]+)\"");

        if (userId == null || role == null) {
            throw new RuntimeException("Risposta JSON non valida: " + body);
        }
        return new LoginResponse(userId, role);
    }

    private static Long extractLong(String s, String regex) {
        Matcher m = Pattern.compile(regex).matcher(s);
        return m.find() ? Long.valueOf(m.group(1)) : null;
    }

    private static String extractString(String s, String regex) {
        Matcher m = Pattern.compile(regex).matcher(s);
        return m.find() ? m.group(1) : null;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
