package ctn.informatica.sca.controller;

import ctn.informatica.sca.dto.GoogleOAuthCallbackRequest;
import ctn.informatica.sca.dto.GoogleOAuthCallbackResponse;
import ctn.informatica.sca.service.GoogleOAuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/google/oauth")
public class GoogleOAuthController {

    private final GoogleOAuthService googleOAuthService;

    public GoogleOAuthController(GoogleOAuthService googleOAuthService) {
        this.googleOAuthService = googleOAuthService;
    }

    @GetMapping("/callback")
    public GoogleOAuthCallbackResponse callback(
            @ModelAttribute GoogleOAuthCallbackRequest request,
            Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        return googleOAuthService.handleCallback(userId, request);
    }
}
