package com.kfarms.security;

import com.kfarms.entity.AppUser;
import com.kfarms.repository.AppUserRepository;
import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final AppUserRepository userRepo;
    private final PasswordResetTokenRepo tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    // Create and email reset token
    public void sendResetEmail(String email) {
        AppUser user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        tokenRepo.save(resetToken);

        String resetLink = "http://localhost:5137/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("KFarms Password Reset");
        message.setText("Salam " + user.getUsername() + ",\n\n"
                + "We received a password reset request for your KFarms account.\n"
                + "Click the link below to reset your password (valid for 30 minutes):\n\n"
                + resetLink + "\n\n"
                + "If you didn’t request this, please ignore this email.\n\n"
                + "KFarms Support");

        mailSender.send(message);
    }

    // Reset password using token
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
        tokenRepo.delete(resetToken); // Invalidate token after use
    }

}
