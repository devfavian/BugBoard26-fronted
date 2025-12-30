package application.model;

public record LoginResponse(Long userID, String role, String token) {}
