package ctn.informatica.sca.dto;

public record AuthErrorResponse(String code, String message, long retryAfterSeconds) {
}