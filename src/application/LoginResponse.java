package application;

public record LoginResponse(Long userID, String role, String token) {}
