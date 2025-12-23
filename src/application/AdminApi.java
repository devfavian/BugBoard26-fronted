package application;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AdminApi {

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(int code, String body) {
            super("HTTP " + code + " Unauthorized" + (body == null || body.isBlank() ? "" : " -> " + body));
        }
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String body) { super(body == null || body.isBlank() ? "Accesso negato (403)" : body); }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String body) { super(body == null || body.isBlank() ? "Email già esistente (409)" : body); }
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static void addAuth(HttpRequest.Builder b) {
        String token = Session.getBearerTokenOrNull();
        if (token == null) {
            throw new UnauthorizedException(401, "Token assente, effettua di nuovo il login.");
        }
        b.setHeader("Authorization", token);
    }

    /**
     * POST /bugboard/admin/register
     * Body: {email, psw, role}
     */
    public static void registerUser(String email, String psw, String role) throws Exception {
        String url = ApiConfig.BASE_URL + "/bugboard/admin/register";

        Map<String, Object> payload = Map.of(
                "email", email,
                "psw", psw,
                "role", role
        );

        String json = MAPPER.writeValueAsString(payload);

        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));

        addAuth(b);

        HttpRequest req = b.build();
        HttpResponse<String> resp = CLIENT.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (resp.statusCode() == 200 || resp.statusCode() == 201) {
            return;
        }
        if (resp.statusCode() == 401) {
            throw new UnauthorizedException(resp.statusCode(), resp.body());
        }
        if (resp.statusCode() == 403) {
            throw new ForbiddenException(resp.body());
        }
        if (resp.statusCode() == 409) {
            throw new ConflictException(resp.body());
        }

        throw new RuntimeException("HTTP " + resp.statusCode() + " - " + resp.body());
    }
}
