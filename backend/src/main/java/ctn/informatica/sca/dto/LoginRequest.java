package ctn.informatica.sca.dto;

public record LoginRequest(String username, String password, Boolean rememberMe) {}