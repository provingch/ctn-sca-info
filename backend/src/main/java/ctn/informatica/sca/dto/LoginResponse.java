package ctn.informatica.sca.dto;

public record LoginResponse(boolean requiere2fa, String tempToken, String accessToken, Integer level) {}