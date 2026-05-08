package com.rentwise.backend.auth;

public interface OtpDeliveryService {
    void deliver(AuthChannel channel, String destination, String code);
}
