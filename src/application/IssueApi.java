package application;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class IssueApi {

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(int code, String body) {
            super("HTTP " + code + " Unauthorized/Forbidden" + (body == null || body.isBlank() ? "" : " -> " + body));
        }
        public UnauthorizedException(String message) { super(message); }
    }
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String body) { super(body == null || body.isBlank() ? "Accesso negato (403)" : body); }
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /** Aggiunge Authorization corretta: "Bearer <jwt>" */
    private static void addAuth(HttpRequest.Builder b) {
        String token = Session.getBearerTokenOrNull();
        if (token == null) {
            throw new UnauthorizedException("Token assente, effettua di nuovo il login.");
        }

        // setHeader evita header duplicati
        b.setHeader("Authorization", token);
    }

    private static void debugRequest(HttpRequest req) {
        String auth = req.headers().firstValue("Authorization").orElse("<missing>");
        String authShort = auth;

        // non stampiamo tutto il token: solo prefisso + lunghezza
        if (auth.toLowerCase().startsWith("bearer ")) {
            String jwt = auth.substring(7);
            String head = jwt.length() > 18 ? jwt.substring(0, 18) + "..." : jwt;
            authShort = "Bearer " + head + " (len=" + jwt.length() + ")";
        }

        System.out.println("=== ISSUE API REQ ===");
        System.out.println(req.method() + " " + req.uri());
        System.out.println("Authorization = " + authShort);
    }

    private static void debugResponse(HttpResponse<String> resp) {
        System.out.println("=== ISSUE API RESP ===");
        System.out.println("status = " + resp.statusCode());
        String body = resp.body() == null ? "" : resp.body();
        if (!body.isBlank()) {
            // stampa max 400 char
            System.out.println("body = " + (body.length() > 400 ? body.substring(0, 400) + "..." : body));
        }
    }

    /**
     * GET /bugboard/issue/view?sort=...
     */
    public static List<IssueItem> getIssues(String sort) throws Exception {
        String url = ApiConfig.BASE_URL + "/bugboard/issue/view?sort=" +
                URLEncoder.encode(sort, StandardCharsets.UTF_8);

        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("Accept", "application/json")
                .GET();

        addAuth(b);

        HttpRequest req = b.build();
        debugRequest(req);

        HttpResponse<String> resp = CLIENT.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        debugResponse(resp);

        if (resp.statusCode() == 200) {
            return MAPPER.readValue(resp.body(), new TypeReference<List<IssueItem>>() {});
        }
        if (resp.statusCode() == 401) {
            throw new UnauthorizedException(resp.statusCode(), resp.body());
        }
        if (resp.statusCode() == 403) {
            throw new ForbiddenException(resp.body());
        }
        throw new RuntimeException("HTTP " + resp.statusCode() + " - " + resp.body());
    }

    /**
     * POST /bugboard/issue/new
     * Body: {title, description, type, priority?, path?}
     */
    public static void createIssue(String title,
                                   String description,
                                   String type,
                                   String priorityOrNull,
                                   String pathOrNull) throws Exception {

        String url = ApiConfig.BASE_URL + "/bugboard/issue/new";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("description", description);
        payload.put("type", type);

        if (priorityOrNull != null && !priorityOrNull.isBlank()) {
            payload.put("priority", priorityOrNull);
        }
        if (pathOrNull != null && !pathOrNull.isBlank()) {
            payload.put("path", pathOrNull);
        }

        String json = MAPPER.writeValueAsString(payload);

        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));

        addAuth(b);

        HttpRequest req = b.build();
        debugRequest(req);

        HttpResponse<String> resp = CLIENT.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        debugResponse(resp);

        if (resp.statusCode() == 201 || resp.statusCode() == 200) {
            return;
        }
        if (resp.statusCode() == 401) {
            throw new UnauthorizedException(resp.statusCode(), resp.body());
        }
        if (resp.statusCode() == 403) {
            throw new ForbiddenException(resp.body());
        }

        throw new RuntimeException("HTTP " + resp.statusCode() + " - " + resp.body());
    }
}
