package com.rentwise.backend.auth;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TotpService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private final String issuer;
    private final int periodSeconds;
    private final int digits;

    public TotpService(
            @Value("${app.totp.issuer:RentWise}") String issuer,
            @Value("${app.totp.period-seconds:30}") int periodSeconds,
            @Value("${app.totp.digits:6}") int digits
    ) {
        this.issuer = issuer;
        this.periodSeconds = periodSeconds;
        this.digits = digits;
    }

    public String issuer() {
        return issuer;
    }

    public String generateSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    public String buildOtpAuthUri(String accountName, String secret) {
        String label = uriEncode(issuer + ":" + accountName);
        String encodedIssuer = uriEncode(issuer);
        return "otpauth://totp/%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d"
                .formatted(label, secret, encodedIssuer, digits, periodSeconds);
    }

    public boolean verify(String secret, String code) {
        return verifyAt(secret, code, Instant.now());
    }

    public boolean verifyAt(String secret, String code, Instant instant) {
        if (!StringUtils.hasText(secret) || !StringUtils.hasText(code)) {
            return false;
        }
        String normalizedCode = code.trim();
        if (!normalizedCode.chars().allMatch(Character::isDigit)) {
            return false;
        }
        long counter = instant.getEpochSecond() / periodSeconds;
        for (long offset = -1; offset <= 1; offset++) {
            if (constantTimeEquals(normalizedCode, generateCode(secret, counter + offset))) {
                return true;
            }
        }
        return false;
    }

    String codeAt(String secret, Instant instant) {
        long counter = instant.getEpochSecond() / periodSeconds;
        return generateCode(secret, counter);
    }

    private String generateCode(String secret, long counter) {
        try {
            byte[] key = base32Decode(secret);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            ByteBuffer buffer = ByteBuffer.allocate(8).putLong(counter);
            byte[] hash = mac.doFinal(buffer.array());
            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int otp = binary % (int) Math.pow(10, digits);
            return String.format(Locale.ROOT, "%0" + digits + "d", otp);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to generate TOTP code", exception);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left.length() != right.length()) {
            return false;
        }
        int result = 0;
        for (int index = 0; index < left.length(); index++) {
            result |= left.charAt(index) ^ right.charAt(index);
        }
        return result == 0;
    }

    private static String uriEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String base32Encode(byte[] data) {
        StringBuilder builder = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte datum : data) {
            buffer <<= 8;
            buffer |= datum & 0xff;
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1f;
                builder.append(BASE32_ALPHABET.charAt(index));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1f;
            builder.append(BASE32_ALPHABET.charAt(index));
        }
        return builder.toString();
    }

    private static byte[] base32Decode(String value) {
        String normalized = value.trim().replace("=", "").toUpperCase(Locale.ROOT);
        int buffer = 0;
        int bitsLeft = 0;
        byte[] output = new byte[normalized.length() * 5 / 8 + 1];
        int outputIndex = 0;
        for (char character : normalized.toCharArray()) {
            int index = BASE32_ALPHABET.indexOf(character);
            if (index < 0) {
                throw new IllegalArgumentException("Invalid TOTP secret");
            }
            buffer <<= 5;
            buffer |= index;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output[outputIndex++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        byte[] result = new byte[outputIndex];
        System.arraycopy(output, 0, result, 0, outputIndex);
        return result;
    }
}
