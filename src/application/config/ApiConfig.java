package application.config;

public final class ApiConfig {
    private ApiConfig() {}

    private static final String DEFAULT_BASE_URL = "http://154.46.187.160:1966";

    public static String baseUrl() {
        String prop = System.getProperty("bugboard.api.baseurl");
        if (prop != null && !prop.isBlank()) return prop.trim();

        String env = System.getenv("BUGBOARD_API_BASE_URL");
        if (env != null && !env.isBlank()) return env.trim();

        return DEFAULT_BASE_URL;
    }
}
