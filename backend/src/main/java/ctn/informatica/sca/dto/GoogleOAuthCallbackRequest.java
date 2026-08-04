package ctn.informatica.sca.dto;

public record GoogleOAuthCallbackRequest(String code, String state, String error) {
}
