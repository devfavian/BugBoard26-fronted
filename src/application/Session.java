package application;

public final class Session {

    private static Long userId;
    private static String role;     // "ADMIN" o "USER"
    private static String token; 
    private static String email;   // JWT "Bearer ..."

    private Session() {}

    // --- userId ---
    public static Long getUserId() { return userId; }
    public static void setUserId(Long id) { userId = id; }

    public static String getEmail() { return email; }
    public static void setEmail(String e) { email = e; }


    // --- role ---
    public static String getRole() { return role; }
    public static void setRole(String r) { role = r; }

    public static boolean isAdmin() {
        return role != null && role.equalsIgnoreCase("ADMIN");
    }

    // --- token (JWT) ---
    public static String getToken() { return token; }
    public static void setToken(String t) { token = t; }

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
