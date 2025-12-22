
package application;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AuthApi {

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException() { super("401 Unauthorized"); }
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static LoginResponse login(String baseUrl, String email, String psw) throws Exception {
        String url = baseUrl + "/bugboard/login";

        Map<String, Object> payload = Map.of(
                "email", email,
                "psw", psw
        );

        String json = MAPPER.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = CLIENT.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (resp.statusCode() == 200) {
            JsonNode n = MAPPER.readTree(resp.body());

            Long userId = n.hasNonNull("userID") ? n.get("userID").asLong() : null;
            String role = n.hasNonNull("role") ? n.get("role").asText() : null;
            String token = n.hasNonNull("token") ? n.get("token").asText() : null;

            if (userId == null || role == null || token == null || token.isBlank()) {
                throw new RuntimeException("LoginResponse non valido: " + resp.body());
            }

            // 🔥 Normalizza: vogliamo SEMPRE "Bearer ..."
            if (!token.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
                token = "Bearer " + token;
            }

            return new LoginResponse(userId, role, token);
        }

        if (resp.statusCode() == 401) {
            throw new UnauthorizedException();
        }

        throw new RuntimeException("HTTP " + resp.statusCode() + " - " + resp.body());
    }
}
