package application;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class JwtDebug {
    private JwtDebug() {}

    public static String payload(String bearerOrJwt) {
        if (bearerOrJwt == null) return "(null)";

        String jwt = bearerOrJwt.trim();
        if (jwt.toLowerCase().startsWith("bearer ")) {
            jwt = jwt.substring(7).trim();
        }

        String[] parts = jwt.split("\\.");
        if (parts.length < 2) return "JWT non valido: " + jwt;

        String payloadB64 = parts[1];
        byte[] decoded = Base64.getUrlDecoder().decode(payloadB64);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    public static String header(String bearerOrJwt) {
        if (bearerOrJwt == null) return "(null)";

        String jwt = bearerOrJwt.trim();
        if (jwt.toLowerCase().startsWith("bearer ")) {
            jwt = jwt.substring(7).trim();
        }

        String[] parts = jwt.split("\\.");
        if (parts.length < 2) return "JWT non valido: " + jwt;

        String headerB64 = parts[0];
        byte[] decoded = Base64.getUrlDecoder().decode(headerB64);
        return new String(decoded, StandardCharsets.UTF_8);
    }
}
