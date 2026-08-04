package ctn.informatica.sca.dto;

public record GoogleOAuthCallbackResponse(
        String status,
        String message,
        String googleEmail,
        Long tokenExpiryEpochSeconds) {
}
