package com.kfarms.security;

import com.kfarms.entity.AppUser;
import com.kfarms.repository.AppUserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final AppUserRepository userRepo;
    private final PasswordResetTokenRepo tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Transactional
    public void sendResetEmail(String email) {
        AppUser user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check for existing token
        PasswordResetToken existingToken = tokenRepo.findByUserId(user.getId())
                .filter(t -> t.getExpiryDate().isAfter(LocalDateTime.now()))
                .orElse(null);

        if (existingToken != null) {
            throw new RuntimeException(
                    "A password reset has already been sent. Please check your email. The link is valid for 30 minutes"
            );
        }

        // Delete old tokens
        tokenRepo.deleteByUser(user.getId());

        // Create new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        tokenRepo.save(resetToken);

        String resetLink = "http://localhost:5173/auth/reset-password?token=" + token;

        // Send email async
        sendEmailAsync(user.getEmail(), user.getUsername(), resetLink);
    }

    @Async
    public void sendEmailAsync(String to, String username, String resetLink) {
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
        } catch (MessagingException e) {
            System.err.println("Failed to send email to " + to);
            e.printStackTrace();
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepo.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        AppUser user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
        tokenRepo.delete(resetToken);
    }
}
