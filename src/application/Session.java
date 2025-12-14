package application;

public class Session {
    private static Long userId;
    private static String role;

    public static Long getUserId() { return userId; }
    public static String getRole() { return role; }

    public static void setUserId(Long id) { userId = id; }
    public static void setRole(String r) { role = r; }
}
