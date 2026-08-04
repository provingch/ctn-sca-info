package ctn.informatica.sca.controller;

import ctn.informatica.sca.dto.ChangePasswordRequest;
import ctn.informatica.sca.dto.ConfirmTotpRequest;
import ctn.informatica.sca.dto.ProfileResponse;
import ctn.informatica.sca.dto.SaveProfileRequest;
import ctn.informatica.sca.dto.SelectUiSpecialtyRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @GetMapping
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    public ProfileResponse getProfile(Authentication authentication) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @PostMapping("/select-ui-specialty")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    public void selectUiSpecialty(
            @RequestBody SelectUiSpecialtyRequest request,
            Authentication authentication) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @PostMapping("/prepare-totp")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    public void prepareTotp(Authentication authentication) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @PostMapping("/confirm-totp")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    public void confirmTotp(
            @RequestBody ConfirmTotpRequest request,
            Authentication authentication) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @PostMapping("/disable-totp")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    public void disableTotp(Authentication authentication) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    public void changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @PostMapping("/save-profile")
    @PreAuthorize("hasAnyRole('LEVEL_1','LEVEL_2','LEVEL_3','LEVEL_4')")
    public void saveProfile(
            @RequestBody SaveProfileRequest request,
            Authentication authentication) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
