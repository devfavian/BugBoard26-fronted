package application;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class IssueApi {

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(int code) { super("HTTP " + code + " Unauthorized/Forbidden"); }
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public static List<IssueItem> getIssues(String sort) throws Exception {
        String url = ApiConfig.BASE_URL + "/bugboard/issue/view?sort=" +
                URLEncoder.encode(sort, StandardCharsets.UTF_8);

        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET();

        String token = Session.getToken();
            if (token != null && !token.isBlank()) {
            b.header("Authorization", token); // token già "Bearer ..."
            }


        HttpResponse<String> resp = CLIENT.send(
                b.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (resp.statusCode() == 200) {
            return MAPPER.readValue(resp.body(), new TypeReference<List<IssueItem>>() {});
        }
        if (resp.statusCode() == 401 || resp.statusCode() == 403) {
            throw new UnauthorizedException(resp.statusCode());
        }
        throw new RuntimeException("HTTP " + resp.statusCode() + " - " + resp.body());
    }
}
