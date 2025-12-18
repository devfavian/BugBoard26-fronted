package application;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AuthApi {

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(int code) { super("HTTP " + code + " Unauthorized/Forbidden"); }
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static LoginResponse login(String baseUrl, String email, String psw) throws Exception {
        // ✅ endpoint coerente col SecurityConfig
        String url = baseUrl + "/bugboard/login";

        String json = "{"
                + "\"email\":\"" + escapeJson(email) + "\","
                + "\"psw\":\"" + escapeJson(psw) + "\""
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (resp.statusCode() == 200) {
            return parseLoginResponse(resp.body());
        }
        if (resp.statusCode() == 401 || resp.statusCode() == 403) {
            throw new UnauthorizedException(resp.statusCode());
        }

        throw new RuntimeException("HTTP " + resp.statusCode() + " - " + resp.body());
    }

    private static LoginResponse parseLoginResponse(String body) throws Exception {
        JsonNode n = MAPPER.readTree(body);

        Long userId = n.hasNonNull("userID") ? n.get("userID").asLong() : null;
        String role  = n.hasNonNull("role")  ? n.get("role").asText()   : null;
        String token = n.hasNonNull("token") ? n.get("token").asText()  : null;

        if (userId == null || role == null || token == null || token.isBlank()) {
            throw new RuntimeException("Risposta login non valida: " + body);
        }

        // Se il backend ti manda solo JWT, lo rendiamo subito pronto per Authorization header
        if (!token.toLowerCase().startsWith("bearer ")) {
            token = "Bearer " + token;
        }

        return new LoginResponse(userId, role, token);
    }

    private static String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
