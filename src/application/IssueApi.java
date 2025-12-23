package application;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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
     * Body: {title, description, type, priority?}
     */
    public static Long createIssue(String title,
                                   String description,
                                   String type,
                                   String priorityOrNull) throws Exception {

        String url = ApiConfig.BASE_URL + "/bugboard/issue/new";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", title);
        payload.put("description", description);
        payload.put("type", type);

        if (priorityOrNull != null && !priorityOrNull.isBlank()) {
            payload.put("priority", priorityOrNull);
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
            Long id = readIssueId(resp.body());
            if (id == null) {
                throw new RuntimeException("Risposta senza id: " + resp.body());
            }
            return id;
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
     * PUT /bugboard/issue/modify/{id}
     * Body: {title, description, type, priority?}
     */
    public static void modifyIssue(Long id,
                                   String title,
                                   String description,
                                   String type,
                                   String priorityOrNull,
                                   String stateOrNull) throws Exception {
        if (id == null) {
            throw new IllegalArgumentException("id mancante");
        }

        String url = ApiConfig.BASE_URL + "/bugboard/issue/modify/" + id;

        Map<String, Object> payload = new LinkedHashMap<>();
        if (title != null && !title.isBlank()) {
            payload.put("title", title);
        }
        if (description != null && !description.isBlank()) {
            payload.put("description", description);
        }
        if (type != null && !type.isBlank()) {
            payload.put("type", type);
        }
        if (priorityOrNull != null && !priorityOrNull.isBlank()) {
            payload.put("priority", priorityOrNull);
        }
        if (stateOrNull != null && !stateOrNull.isBlank()) {
            payload.put("state", stateOrNull);
        }

        String json = MAPPER.writeValueAsString(payload);

        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));

        addAuth(b);

        HttpRequest req = b.build();
        debugRequest(req);

        HttpResponse<String> resp = CLIENT.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        debugResponse(resp);

        if (resp.statusCode() == 200) {
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

    /**
     * POST /bugboard/issue/{id}/image
     * Multipart: {file}
     */
    public static String uploadIssueImage(Long issueId, File file) throws Exception {
        if (issueId == null) {
            throw new IllegalArgumentException("issueId mancante");
        }
        if (file == null) {
            throw new IllegalArgumentException("file mancante");
        }
        validateImageFile(file);

        String url = ApiConfig.BASE_URL + "/bugboard/issue/" + issueId + "/image";
        String boundary = "BugBoardBoundary" + System.currentTimeMillis();

        HttpRequest.BodyPublisher body = buildMultipartBody(boundary, file);

        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Accept", "application/json")
                .POST(body);

        addAuth(b);

        HttpRequest req = b.build();
        debugRequest(req);

        HttpResponse<String> resp = CLIENT.send(
                req,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        debugResponse(resp);

        if (resp.statusCode() == 200 || resp.statusCode() == 201) {
            return readImagePath(resp.body());
        }
        if (resp.statusCode() == 401) {
            throw new UnauthorizedException(resp.statusCode(), resp.body());
        }
        if (resp.statusCode() == 403) {
            throw new ForbiddenException(resp.body());
        }

        throw new RuntimeException("HTTP " + resp.statusCode() + " - " + resp.body());
    }

    private static Long readIssueId(String body) throws IOException {
        if (body == null || body.isBlank()) return null;
        JsonNode node = MAPPER.readTree(body);
        return node.hasNonNull("id") ? node.get("id").asLong() : null;
    }

    private static String readImagePath(String body) throws IOException {
        if (body == null || body.isBlank()) return null;
        JsonNode node = MAPPER.readTree(body);
        return node.hasNonNull("path") ? node.get("path").asText() : null;
    }

    private static void validateImageFile(File file) {
        String name = file.getName().toLowerCase();
        if (!(name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".webp"))) {
            throw new IllegalArgumentException("Formato immagine non supportato: " + file.getName());
        }
    }

    private static String contentTypeForImage(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg")) return "image/jpeg";
        if (name.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    private static HttpRequest.BodyPublisher buildMultipartBody(String boundary, File file) throws IOException {
        List<byte[]> byteArrays = new ArrayList<>();
        String filename = file.getName();
        String contentType = contentTypeForImage(file);

        byteArrays.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        byteArrays.add(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        byteArrays.add(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        byteArrays.add(Files.readAllBytes(file.toPath()));
        byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
        byteArrays.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }

    /**
     * GET image url with Authorization.
     */
    public static byte[] downloadIssueImage(String url) throws Exception {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL immagine mancante");
        }

        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "image/*")
                .GET();

        addAuth(b);

        HttpRequest req = b.build();
        debugRequest(req);

        HttpResponse<byte[]> resp = CLIENT.send(
                req,
                HttpResponse.BodyHandlers.ofByteArray()
        );

        if (resp.statusCode() == 200) {
            return resp.body();
        }
        if (resp.statusCode() == 401) {
            throw new UnauthorizedException(resp.statusCode(), "");
        }
        if (resp.statusCode() == 403) {
            throw new ForbiddenException("");
        }

        throw new RuntimeException("HTTP " + resp.statusCode());
    }

    /**
     * Try download from provided URL, then fallback to /bugboard/issue/{id}/image if available.
     */
    public static byte[] downloadIssueImageWithFallback(Long issueId, String url) throws Exception {
        Exception last = null;
        if (issueId != null) {
            String primaryUrl = ApiConfig.BASE_URL + "/bugboard/issue/" + issueId + "/image";
            try {
                return downloadIssueImage(primaryUrl);
            } catch (Exception ex) {
                last = ex;
            }
        }
        if (url != null && !url.isBlank()) {
            try {
                return downloadIssueImage(url);
            } catch (Exception ex) {
                if (last != null) {
                    ex.addSuppressed(last);
                }
                throw ex;
            }
        }
        if (last != null) {
            throw last;
        }
        throw new IllegalArgumentException("URL e issueId mancanti");
    }
}
