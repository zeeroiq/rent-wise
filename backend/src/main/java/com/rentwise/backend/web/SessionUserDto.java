package com.rentwise.backend.web;

public record SessionUserDto(
        Long id,
        String displayName,
        String email,
        String mobileNumber,
        String avatarUrl
) {
}
