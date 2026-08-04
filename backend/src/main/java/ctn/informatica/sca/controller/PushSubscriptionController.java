package ctn.informatica.sca.controller;

import ctn.informatica.sca.dto.PushSubscriptionResponse;
import ctn.informatica.sca.dto.PushSubscriptionSaveRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push-subscription")
public class PushSubscriptionController {

    @GetMapping
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    public PushSubscriptionResponse getPushSubscription(Authentication authentication) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    public void savePushSubscription(
            @RequestBody PushSubscriptionSaveRequest request,
            Authentication authentication) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @PostMapping("/unsubscribe")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    public void unsubscribePushSubscription(Authentication authentication) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    public void testPushSubscription(Authentication authentication) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
