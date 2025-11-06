package com.kfarms.security;

import com.kfarms.entity.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/forgot-password")
    public ApiResponse<String> forgotPassword(@RequestParam String email) {
        passwordResetService.sendResetEmail(email);
        return new ApiResponse<>(true, "Password reset link sent successfully", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<String> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        passwordResetService.resetPassword(token, newPassword);
        return new ApiResponse<>(true, "Password reset successfully", null);
    }
}
