package com.rentwise.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/otp/request")
    public OtpChallengeResponse requestOtp(@Valid @RequestBody OtpRequestCommand command) {
        return authService.requestOtp(command);
    }

    @PostMapping("/otp/verify")
    public AuthSessionResponse verifyOtp(
            @Valid @RequestBody OtpVerifyCommand command,
            HttpServletRequest request
    ) {
        return authService.verifyOtp(command, request);
    }

    @GetMapping("/session")
    public AuthSessionResponse session(Authentication authentication) {
        return authService.currentSession(authentication);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
