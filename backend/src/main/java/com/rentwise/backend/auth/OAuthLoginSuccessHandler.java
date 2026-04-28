package com.rentwise.backend.auth;

import com.rentwise.backend.user.AppUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthService authService;
    private final String frontendBaseUrl;

    public OAuthLoginSuccessHandler(AuthService authService, @Value("${app.frontend-base-url}") String frontendBaseUrl) {
        this.authService = authService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = token.getPrincipal();
        AppUser user = authService.upsertOauthUser(token.getAuthorizedClientRegistrationId(), oauthUser.getAttributes());
        authService.authenticate(request, user);
        response.sendRedirect(frontendBaseUrl + "?auth=success");
    }
}
