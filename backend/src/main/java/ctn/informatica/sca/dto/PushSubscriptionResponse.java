package ctn.informatica.sca.dto;

public record PushSubscriptionResponse(
        String publicKey,
        boolean subscribed
) {
}
