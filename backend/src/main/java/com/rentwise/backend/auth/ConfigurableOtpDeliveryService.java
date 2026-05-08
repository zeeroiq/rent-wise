package com.rentwise.backend.auth;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ConfigurableOtpDeliveryService implements OtpDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(ConfigurableOtpDeliveryService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final Environment environment;
    private final HttpClient httpClient;
    private final String smsAccountSid;
    private final String smsAuthToken;
    private final String smsFromNumber;
    private final String otpFromEmail;
    private final String telegramBotToken;
    private final String signalAccount;
    private final String signalCliPath;

    public ConfigurableOtpDeliveryService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            Environment environment,
            @Value("${app.sms.account-sid:}") String smsAccountSid,
            @Value("${app.sms.auth-token:}") String smsAuthToken,
            @Value("${app.sms.from-number:}") String smsFromNumber,
            @Value("${app.mail.from:rentwise@zeeroiq.com}") String otpFromEmail,
            @Value("${app.telegram.bot-token:}") String telegramBotToken,
            @Value("${app.signal.account:}") String signalAccount,
            @Value("${app.signal.cli-path:signal-cli}") String signalCliPath
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.environment = environment;
        this.smsAccountSid = smsAccountSid;
        this.smsAuthToken = smsAuthToken;
        this.smsFromNumber = smsFromNumber;
        this.otpFromEmail = otpFromEmail;
        this.telegramBotToken = telegramBotToken;
        this.signalAccount = signalAccount;
        this.signalCliPath = signalCliPath;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void deliver(AuthChannel channel, String destination, String code) {
        switch (channel) {
            case EMAIL -> deliverEmail(destination, code);
            case MOBILE -> deliverSms(destination, code);
            case TELEGRAM -> deliverTelegram(destination, code);
            case SIGNAL -> deliverSignal(destination, code);
            case TOTP -> {
                // TOTP is verified locally and is not delivered.
            }
        }
    }

    private void deliverEmail(String destination, String code) {
        if (!hasText(getMailHost())) {
            if (isDevOtpVisible()) {
                log.info("SMTP not configured, skipping email OTP delivery to {}", destination);
                return;
            }
            throw new IllegalStateException("Email OTP delivery requires SMTP configuration");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(destination);
        message.setFrom(hasText(otpFromEmail) ? otpFromEmail : defaultFromAddress());
        message.setSubject("RentWise verification code");
        message.setText("""
                Your RentWise verification code is %s.

                This code expires in 10 minutes.
                """.formatted(code));
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            if (isDevOtpVisible()) {
                log.info("JavaMailSender unavailable, skipping email OTP delivery to {}", destination);
                return;
            }
            throw new IllegalStateException("Email OTP delivery requires a JavaMailSender bean");
        }
        mailSender.send(message);
    }

    private void deliverSms(String destination, String code) {
        if (!hasText(smsAccountSid) || !hasText(smsAuthToken) || !hasText(smsFromNumber)) {
            if (isDevOtpVisible()) {
                log.info("SMS gateway not configured, skipping phone OTP delivery to {}", destination);
                return;
            }
            throw new IllegalStateException("Phone OTP delivery requires SMS gateway configuration");
        }

        String body = "RentWise verification code: %s. Expires in 10 minutes.".formatted(code);
        String form = "To=%s&From=%s&Body=%s".formatted(
                encode(destination),
                encode(smsFromNumber),
                encode(body)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json".formatted(smsAccountSid)))
                .header("Authorization", basicAuth(smsAccountSid, smsAuthToken))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("SMS gateway returned HTTP " + response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("SMS delivery interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("SMS delivery failed", exception);
        }
    }

    private void deliverTelegram(String destination, String code) {
        if (!hasText(telegramBotToken)) {
            if (isDevOtpVisible()) {
                log.info("Telegram bot not configured, skipping OTP delivery to {}", destination);
                return;
            }
            throw new IllegalStateException("Telegram OTP delivery requires a bot token");
        }

        String message = "RentWise verification code: %s. Expires in 10 minutes.".formatted(code);
        String uri = "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s"
                .formatted(
                        telegramBotToken,
                        encode(destination),
                        encode(message)
                );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("Telegram API returned HTTP " + response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Telegram delivery interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Telegram delivery failed", exception);
        }
    }

    private void deliverSignal(String destination, String code) {
        if (!hasText(signalAccount)) {
            if (isDevOtpVisible()) {
                log.info("Signal account not configured, skipping OTP delivery to {}", destination);
                return;
            }
            throw new IllegalStateException("Signal OTP delivery requires a configured account");
        }
        ProcessBuilder processBuilder = new ProcessBuilder(
                signalCliPath,
                "-a",
                signalAccount,
                "send",
                "-m",
                "RentWise verification code: %s. Expires in 10 minutes.".formatted(code),
                destination
        );
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("Signal CLI returned exit code %d: %s".formatted(exitCode, output.trim()));
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Signal delivery interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Signal delivery failed", exception);
        }
    }

    private boolean isDevOtpVisible() {
        return Boolean.parseBoolean(environment.getProperty("app.otp.expose-dev-code", "false"));
    }

    private String getMailHost() {
        return environment.getProperty("spring.mail.host");
    }

    private String defaultFromAddress() {
        String username = environment.getProperty("spring.mail.username");
        if (hasText(username) && username.contains("@")) {
            return username;
        }
        return "no-reply@rentwise.local";
    }

    private static String basicAuth(String username, String password) {
        String raw = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}
