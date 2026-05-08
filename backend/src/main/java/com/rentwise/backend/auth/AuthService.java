package com.rentwise.backend.auth;

import com.rentwise.backend.user.AppUser;
import com.rentwise.backend.user.AppUserRepository;
import com.rentwise.backend.web.SessionUserDto;
import jakarta.servlet.http.HttpServletRequest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppUserRepository appUserRepository;
    private final OtpChallengeRepository otpChallengeRepository;
    private final OtpDeliveryService otpDeliveryService;
    private final Environment environment;
    private final int otpLength;
    private final int otpTtlMinutes;
    private final boolean exposeDevCode;

    public AuthService(
            AppUserRepository appUserRepository,
            OtpChallengeRepository otpChallengeRepository,
            OtpDeliveryService otpDeliveryService,
            Environment environment,
            @Value("${app.otp.length}") int otpLength,
            @Value("${app.otp.ttl-minutes}") int otpTtlMinutes,
            @Value("${app.otp.expose-dev-code}") boolean exposeDevCode
    ) {
        this.appUserRepository = appUserRepository;
        this.otpChallengeRepository = otpChallengeRepository;
        this.otpDeliveryService = otpDeliveryService;
        this.environment = environment;
        this.otpLength = otpLength;
        this.otpTtlMinutes = otpTtlMinutes;
        this.exposeDevCode = exposeDevCode;
    }

    public OtpChallengeResponse requestOtp(OtpRequestCommand command) {
        String destination = command.destination().trim();
        validateDestination(command.channel(), destination);
        String code = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpTtlMinutes);
        OtpChallenge challenge = otpChallengeRepository.save(new OtpChallenge(command.channel(), destination, code, expiresAt));
        otpDeliveryService.deliver(command.channel(), destination, code);
        if (exposeDevCode) {
            log.info("DEV OTP for {} {} is {}", command.channel(), destination, code);
        }
        return new OtpChallengeResponse(challenge.getId(), destination, expiresAt, exposeDevCode ? code : null);
    }

    public AuthSessionResponse verifyOtp(OtpVerifyCommand command, HttpServletRequest request) {
        OtpChallenge challenge = otpChallengeRepository.findById(command.challengeId())
                .orElseThrow(() -> new IllegalArgumentException("OTP challenge not found"));
        LocalDateTime now = LocalDateTime.now();
        if (challenge.isConsumed()) {
            throw new IllegalArgumentException("OTP challenge already used");
        }
        if (challenge.isExpired(now)) {
            throw new IllegalArgumentException("OTP challenge expired");
        }
        if (!challenge.getCode().equals(command.code().trim())) {
            throw new IllegalArgumentException("OTP code does not match");
        }

        AppUser user = switch (challenge.getChannel()) {
            case EMAIL -> appUserRepository.findByEmailIgnoreCase(challenge.getDestination())
                    .orElseGet(() -> appUserRepository.save(new AppUser(
                            resolveDisplayName(command.displayName(), challenge.getDestination()),
                            challenge.getDestination(),
                            null,
                            AuthProvider.OTP_EMAIL,
                            null
                    )));
            case MOBILE -> appUserRepository.findByMobileNumber(challenge.getDestination())
                    .orElseGet(() -> appUserRepository.save(new AppUser(
                            resolveDisplayName(command.displayName(), challenge.getDestination()),
                            null,
                            challenge.getDestination(),
                            AuthProvider.OTP_MOBILE,
                            null
                    )));
        };

        challenge.markConsumed(now);
        authenticate(request, user);
        return buildSessionResponse(user);
    }

    public AuthSessionResponse currentSession(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof RentwisePrincipal principal)) {
            return new AuthSessionResponse(null, configuredProviders(), exposeDevCode);
        }
        AppUser user = appUserRepository.findById(principal.userId()).orElse(null);
        if (user == null) {
            return new AuthSessionResponse(null, configuredProviders(), exposeDevCode);
        }
        return buildSessionResponse(user);
    }

    public void logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    public AppUser upsertOauthUser(String registrationId, Map<String, Object> attributes) {
        String email = trim((String) attributes.get("email"));
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("OAuth login did not provide an email address");
        }
        String displayName = firstNonBlank(
                trim((String) attributes.get("name")),
                trim((String) attributes.get("given_name")),
                email.substring(0, email.indexOf('@'))
        );
        String avatarUrl = firstNonBlank(trim((String) attributes.get("picture")), trim((String) attributes.get("avatar_url")));
        AuthProvider provider = "facebook".equalsIgnoreCase(registrationId) ? AuthProvider.FACEBOOK : AuthProvider.GOOGLE;

        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> new AppUser(displayName, email, null, provider, avatarUrl));
        user.setDisplayName(displayName);
        user.setAvatarUrl(avatarUrl);
        user.setAuthProvider(provider);
        return appUserRepository.save(user);
    }

    public void authenticate(HttpServletRequest request, AppUser user) {
        RentwisePrincipal principal = RentwisePrincipal.fromUser(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    private AuthSessionResponse buildSessionResponse(AppUser user) {
        return new AuthSessionResponse(toSessionUser(user), configuredProviders(), exposeDevCode);
    }

    private SessionUserDto toSessionUser(AppUser user) {
        return new SessionUserDto(user.getId(), user.getDisplayName(), user.getEmail(), user.getMobileNumber(), user.getAvatarUrl(), user.isAdmin());
    }

    private List<String> configuredProviders() {
        List<String> providers = new ArrayList<>();
        boolean google = hasConfiguredOauth("spring.security.oauth2.client.registration.google.client-id")
                || hasConfiguredOauth("GOOGLE_CLIENT_ID");
        boolean facebook = hasConfiguredOauth("spring.security.oauth2.client.registration.facebook.client-id")
                || hasConfiguredOauth("FACEBOOK_CLIENT_ID");
        if (google) {
            providers.add("google");
        }
        if (facebook) {
            providers.add("facebook");
        }
        return providers;
    }

    private boolean hasConfiguredOauth(String propertyName) {
        return StringUtils.hasText(environment.getProperty(propertyName));
    }

    private String resolveDisplayName(String displayName, String destination) {
        if (StringUtils.hasText(displayName)) {
            return displayName.trim();
        }
        String seed = destination.toLowerCase(Locale.ROOT);
        if (seed.contains("@")) {
            return seed.substring(0, seed.indexOf('@'));
        }
        return "tenant-" + seed.substring(Math.max(0, seed.length() - 4));
    }

    private String generateOtp() {
        int max = (int) Math.pow(10, otpLength);
        int min = max / 10;
        return Integer.toString(RANDOM.nextInt(min, max));
    }

    private void validateDestination(AuthChannel channel, String destination) {
        if (channel == AuthChannel.EMAIL && !destination.contains("@")) {
            throw new IllegalArgumentException("Provide a valid email address");
        }
        if (channel == AuthChannel.MOBILE && destination.replaceAll("\\D", "").length() < 8) {
            throw new IllegalArgumentException("Provide a valid mobile number");
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
