package application.core;

public final class Session {

    private static Long userId;
    private static String role;
    private static String token;
    private static String email;

    private Session() {}

    public static Long getUserId() { return userId; }
    public static void setUserId(Long id) { userId = id; }

    public static String getEmail() { return email; }
    public static void setEmail(String e) { email = e; }


    public static String getRole() { return role; }
    public static void setRole(String r) { role = r; }

    public static boolean isAdmin() {
        return role != null && role.equalsIgnoreCase("ADMIN");
    }

    public static String getToken() { return token; }
    public static void setToken(String t) { token = t; }
    public static String getBearerTokenOrNull() {
        if (token == null || token.isBlank()) return null;
        String tok = token.trim();
        if (!tok.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length())) {
            tok = "Bearer " + tok;
        }
        return tok;
    }

    public static boolean isLoggedIn() {
        return token != null && !token.isBlank() && userId != null;
    }

    public static void clear() {
    userId = null;
    role = null;
    token = null;
    email = null;
    }

}
