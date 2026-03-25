package com.kfarms.security;

import com.kfarms.dto.AuthSignupRequest;
import com.kfarms.dto.ContactVerificationRequest;
import com.kfarms.dto.VerificationResendRequest;
import com.kfarms.entity.AppUser;
import com.kfarms.entity.Role;
import com.kfarms.repository.AppUserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContactVerificationService {

    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate = new RestTemplate();
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${kfarms.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${kfarms.sms.webhook-url:}")
    private String smsWebhookUrl;

    @Value("${kfarms.sms.api-key:}")
    private String smsApiKey;

    @Value("${kfarms.sms.sender:KFarms}")
    private String smsSender;

    @Value("${kfarms.auth.verification.code-length:6}")
    private int codeLength;

    @Value("${kfarms.auth.verification.ttl-minutes:15}")
    private int ttlMinutes;

    @Value("${kfarms.auth.verification.preview-enabled:true}")
    private boolean previewEnabled;

    @Transactional
    public Map<String, Object> register(AuthSignupRequest request) {
        String email = AccountSecuritySupport.normalizeEmail(request.email());
        String username = requireText(request.username(), "Username is required.");
        String phoneNumber = AccountSecuritySupport.normalizePhoneNumber(request.phoneNumber());

        if (!AccountSecuritySupport.isValidEmail(email)) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }

        if (!AccountSecuritySupport.isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Please enter a valid phone number.");
        }

        AccountSecuritySupport.validatePassword(request.password(), AccountSecuritySupport.MIN_PASSWORD_LENGTH);

        if (userRepo.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (userRepo.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }

        if (userRepo.findByPhoneNumber(phoneNumber).isPresent()) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setPhoneVerified(false);
        user.setPlatformAccess(false);

        Map<String, String> preview = new LinkedHashMap<>();
        String emailCode = issueEmailCode(user);
        String phoneCode = issuePhoneCode(user);
        userRepo.save(user);

        if (previewEnabled) {
            preview.put("emailCode", emailCode);
            preview.put("phoneCode", phoneCode);
        }

        sendEmailCode(user, emailCode);
        sendPhoneCode(user, phoneCode);

        return buildVerificationPayload(user, preview);
    }

    @Transactional
    public Map<String, Object> verify(ContactVerificationRequest request) {
        AppUser user = requireUserByEmail(request.email());

        if (!isEmailVerified(user)) {
            verifyCode(
                    valueOrEmpty(request.emailCode()),
                    user.getEmailVerificationCode(),
                    user.getEmailVerificationExpiresAt(),
                    "email"
            );
            user.setEmailVerified(true);
            user.setEmailVerificationCode(null);
            user.setEmailVerificationExpiresAt(null);
        }

        if (!isPhoneVerified(user)) {
            verifyCode(
                    valueOrEmpty(request.phoneCode()),
                    user.getPhoneVerificationCode(),
                    user.getPhoneVerificationExpiresAt(),
                    "SMS"
            );
            user.setPhoneVerified(true);
            user.setPhoneVerificationCode(null);
            user.setPhoneVerificationExpiresAt(null);
        }

        AppUser saved = userRepo.save(user);
        return buildVerificationPayload(saved, Map.of());
    }

    @Transactional
    public Map<String, Object> resend(VerificationResendRequest request) {
        AppUser user = requireUserByEmail(request.email());
        String channel = normalizeChannel(request.channel());
        Map<String, String> preview = new LinkedHashMap<>();

        if (("ALL".equals(channel) || "EMAIL".equals(channel)) && !isEmailVerified(user)) {
            String emailCode = issueEmailCode(user);
            if (previewEnabled) {
                preview.put("emailCode", emailCode);
            }
            sendEmailCode(user, emailCode);
        }

        if (("ALL".equals(channel) || "SMS".equals(channel)) && !isPhoneVerified(user)) {
            String phoneCode = issuePhoneCode(user);
            if (previewEnabled) {
                preview.put("phoneCode", phoneCode);
            }
            sendPhoneCode(user, phoneCode);
        }

        AppUser saved = userRepo.save(user);
        return buildVerificationPayload(saved, preview);
    }

    public boolean isFullyVerified(AppUser user) {
        return isEmailVerified(user) && isPhoneVerified(user);
    }

    public Map<String, Object> buildVerificationPayload(AppUser user, Map<String, String> preview) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("email", user.getEmail());
        payload.put("maskedEmail", AccountSecuritySupport.maskEmail(user.getEmail()));
        payload.put("maskedPhoneNumber", AccountSecuritySupport.maskPhoneNumber(user.getPhoneNumber()));
        payload.put("emailVerified", isEmailVerified(user));
        payload.put("phoneVerified", isPhoneVerified(user));
        payload.put("verificationRequired", !isFullyVerified(user));
        if (previewEnabled && preview != null && !preview.isEmpty()) {
            payload.put("preview", preview);
        }
        return payload;
    }

    private AppUser requireUserByEmail(String email) {
        String normalizedEmail = AccountSecuritySupport.normalizeEmail(email);
        return userRepo.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));
    }

    private boolean isEmailVerified(AppUser user) {
        return !Boolean.FALSE.equals(user.getEmailVerified());
    }

    private boolean isPhoneVerified(AppUser user) {
        if (!StringUtils.hasText(user.getPhoneNumber())) {
            return true;
        }
        return !Boolean.FALSE.equals(user.getPhoneVerified());
    }

    private String issueEmailCode(AppUser user) {
        String code = generateCode();
        user.setEmailVerificationCode(code);
        user.setEmailVerificationExpiresAt(LocalDateTime.now().plusMinutes(Math.max(ttlMinutes, 5)));
        return code;
    }

    private String issuePhoneCode(AppUser user) {
        String code = generateCode();
        user.setPhoneVerificationCode(code);
        user.setPhoneVerificationExpiresAt(LocalDateTime.now().plusMinutes(Math.max(ttlMinutes, 5)));
        return code;
    }

    private String generateCode() {
        int safeLength = Math.max(codeLength, 4);
        int upperBound = (int) Math.pow(10, safeLength);
        String template = "%0" + safeLength + "d";
        return String.format(Locale.ROOT, template, secureRandom.nextInt(upperBound));
    }

    private void verifyCode(String received, String expected, LocalDateTime expiresAt, String label) {
        if (!StringUtils.hasText(received)) {
            throw new IllegalArgumentException("Enter the " + label + " verification code.");
        }

        if (!StringUtils.hasText(expected) || expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("The " + label + " verification code has expired. Request a new one.");
        }

        if (!expected.equals(received.trim())) {
            throw new IllegalArgumentException("The " + label + " verification code is incorrect.");
        }
    }

    private void sendEmailCode(AppUser user, String code) {
        if (!isEmailDeliveryConfigured()) {
            if (previewEnabled) {
                log.info("Email verification preview for {}: {}", user.getEmail(), code);
                return;
            }
            throw new IllegalStateException("Email verification is not configured right now.");
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String html = "<div style='font-family:sans-serif;color:#0f172a;'>"
                    + "<h2>Verify your KFarms email</h2>"
                    + "<p>Hello " + user.getUsername() + ",</p>"
                    + "<p>Your KFarms verification code is:</p>"
                    + "<p style='font-size:28px;font-weight:700;letter-spacing:0.22em;'>" + code + "</p>"
                    + "<p>This code expires in " + Math.max(ttlMinutes, 5) + " minutes.</p>"
                    + "<p>If you did not request this, you can ignore it.</p>"
                    + "<br><p>KFarms Security</p></div>";

            helper.setTo(user.getEmail());
            helper.setSubject("KFarms email verification code");
            helper.setText(html, true);

            mailSender.send(mimeMessage);
        } catch (MailException | MessagingException ex) {
            if (previewEnabled) {
                log.warn("Email verification fallback preview for {} after mail error: {}", user.getEmail(), code);
                return;
            }
            throw new IllegalStateException("We could not send the email verification code right now.");
        }
    }

    private void sendPhoneCode(AppUser user, String code) {
        String recipient = AccountSecuritySupport.normalizePhoneNumber(user.getPhoneNumber());
        if (!StringUtils.hasText(recipient)) {
            throw new IllegalArgumentException("Please enter a valid phone number.");
        }

        if (!isSmsConfigured()) {
            if (previewEnabled) {
                log.info("SMS verification preview for {}: {}", recipient, code);
                return;
            }
            throw new IllegalStateException("SMS verification is not configured right now.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (StringUtils.hasText(smsApiKey)) {
                headers.setBearerAuth(smsApiKey.trim());
            }

            Map<String, Object> payload = Map.of(
                    "to", recipient,
                    "message", "Your KFarms verification code is " + code + ". It expires in " + Math.max(ttlMinutes, 5) + " minutes.",
                    "sender", StringUtils.hasText(smsSender) ? smsSender.trim() : "KFarms",
                    "purpose", "account-verification",
                    "email", user.getEmail()
            );

            restTemplate.postForEntity(smsWebhookUrl.trim(), new HttpEntity<>(payload, headers), String.class);
        } catch (Exception ex) {
            if (previewEnabled) {
                log.warn("SMS verification fallback preview for {} after delivery error: {}", recipient, code);
                return;
            }
            throw new IllegalStateException("We could not send the SMS verification code right now.");
        }
    }

    private boolean isEmailDeliveryConfigured() {
        return StringUtils.hasText(smtpUsername);
    }

    private boolean isSmsConfigured() {
        return smsEnabled && StringUtils.hasText(smsWebhookUrl);
    }

    private String normalizeChannel(String value) {
        String normalized = valueOrEmpty(value).toUpperCase(Locale.ROOT);
        if ("PHONE".equals(normalized)) {
            return "SMS";
        }
        if ("ALL".equals(normalized) || "EMAIL".equals(normalized) || "SMS".equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("Verification channel must be EMAIL, SMS, or ALL.");
    }

    private String requireText(String value, String message) {
        String normalized = valueOrEmpty(value);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
