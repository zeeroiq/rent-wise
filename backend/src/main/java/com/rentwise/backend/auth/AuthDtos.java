package com.rentwise.backend.auth;

import com.rentwise.backend.web.SessionUserDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

record OtpRequestCommand(
        @NotNull AuthChannel channel,
        @NotBlank String destination,
        String displayName
) {
}

record OtpChallengeResponse(
        Long challengeId,
        String destination,
        LocalDateTime expiresAt,
        String devCode
) {
}

record OtpVerifyCommand(
        @NotNull Long challengeId,
        @NotBlank String code,
        String displayName
) {
}

record AuthSessionResponse(
        SessionUserDto user,
        List<String> oauthProviders,
        boolean devOtpVisible
) {
}
