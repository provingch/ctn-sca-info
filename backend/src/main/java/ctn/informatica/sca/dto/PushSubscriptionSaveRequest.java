package ctn.informatica.sca.dto;

public record PushSubscriptionSaveRequest(
        String endpoint,
        String p256dh,
        String auth
) {
}
