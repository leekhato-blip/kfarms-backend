package com.kfarms.security;

import com.kfarms.entity.AppUser;
import com.kfarms.repository.AppUserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final AppUserRepository userRepo;
    private final PasswordResetTokenRepo tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${kfarms.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Transactional
    public void sendResetEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return;
        }

        AppUser user = userRepo.findByEmail(normalizedEmail).orElse(null);
        if (user == null) {
            return;
        }

        tokenRepo.deleteByUser(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        tokenRepo.save(resetToken);

        String resetLink = buildResetLink(token);

        sendEmail(user.getEmail(), user.getUsername(), resetLink);
    }

    public void sendEmail(String to, String username, String resetLink) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String html = "<div style='font-family: sans-serif; color:#333;'>"
                    + "<h2>KFarms Password Reset</h2>"
                    + "<p>Hello " + username + ",</p>"
                    + "<p>We received a password reset request for your KFarms account.</p>"
                    + "<p><a href='" + resetLink + "' "
                    + "style='background:#2563EB;color:white;padding:10px 20px;border-radius:6px;"
                    + "text-decoration:none;font-weight:bold;'>Reset Password</a></p>"
                    + "<p>This link will expire in 30 minutes. If you didn’t request this, ignore this email.</p>"
                    + "<br><p>– KFarms Support</p></div>";

//            helper.setFrom("MS_xxxx@your-subdomain.mlsender.net");
            helper.setTo(to);
            helper.setSubject("KFarms Password Reset");
            helper.setText(html, true);

            mailSender.send(mimeMessage);
            System.out.println("Password reset email sent to " + to);
        } catch (MailException | MessagingException e) {
            throw new IllegalStateException("We could not send the reset email right now. Please try again.");
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String normalizedToken = token == null ? "" : token.trim();
        String normalizedPassword = newPassword == null ? "" : newPassword.trim();

        if (!StringUtils.hasText(normalizedToken)) {
            throw new IllegalArgumentException("Invalid or expired reset link.");
        }

        if (normalizedPassword.length() < 8) {
            throw new IllegalArgumentException("Use at least 8 characters for your new password.");
        }

        PasswordResetToken resetToken = tokenRepo.findByToken(normalizedToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset link."));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepo.delete(resetToken);
            throw new IllegalArgumentException("This reset link has expired. Request a new one.");
        }

        AppUser user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(normalizedPassword));
        userRepo.save(user);
        tokenRepo.delete(resetToken);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private String buildResetLink(String token) {
        String baseUrl = StringUtils.hasText(frontendBaseUrl)
                ? frontendBaseUrl.trim().replaceAll("/+$", "")
                : "http://localhost:5173";
        return baseUrl + "/auth/reset-password?token=" + token;
    }
}
