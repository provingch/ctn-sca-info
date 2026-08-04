package ctn.informatica.sca.dto;

public record Verify2faRequest(String tempToken, String code, Boolean rememberMe) {}