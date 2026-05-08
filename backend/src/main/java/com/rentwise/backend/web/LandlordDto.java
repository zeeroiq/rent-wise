package com.rentwise.backend.web;

public record LandlordDto(
        Long id,
        String name,
        String email,
        String phoneNumber,
        String managementStyle
) {
}
