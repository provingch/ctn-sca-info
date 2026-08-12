package ctn.informatica.sca.controller;

import ctn.informatica.sca.dto.GoogleOAuthCallbackRequest;
import ctn.informatica.sca.dto.GoogleOAuthCallbackResponse;
import ctn.informatica.sca.service.GoogleOAuthService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping("/authorize-url")
    public java.util.Map<String, String> authorizeUrl(Authentication authentication) {
        // require user id to ensure caller is authenticated
        ApiAuth.requireUserId(authentication);
        String url = googleOAuthService.buildAuthorizeUrl();
        return java.util.Collections.singletonMap("url", url);
    }

    @PostMapping("/callback")
    public GoogleOAuthCallbackResponse callbackPost(@RequestBody GoogleOAuthCallbackRequest request, Authentication authentication) {
        int userId = ApiAuth.requireUserId(authentication);
        return googleOAuthService.handleCallback(userId, request);
    }
}
