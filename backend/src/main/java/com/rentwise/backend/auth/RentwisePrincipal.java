package com.rentwise.backend.auth;

import com.rentwise.backend.user.AppUser;
import java.io.Serializable;

public record RentwisePrincipal(
        Long userId,
        String displayName,
        String email,
        String mobileNumber,
        boolean isAdmin
) implements Serializable {
    public static RentwisePrincipal fromUser(AppUser user) {
        return new RentwisePrincipal(user.getId(), user.getDisplayName(), user.getEmail(), user.getMobileNumber(), user.isAdmin());
    }
}
