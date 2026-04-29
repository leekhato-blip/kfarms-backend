package com.kfarms.controller;

import com.kfarms.dto.AuthSignupRequest;
import com.kfarms.dto.ContactVerificationRequest;
import com.kfarms.dto.LoginRequest;
import com.kfarms.dto.LoginResponse;
import com.kfarms.dto.UserDto;
import com.kfarms.dto.VerificationResendRequest;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.AppUser;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.security.AuthCookieFactory;
import com.kfarms.security.ContactVerificationService;
import com.kfarms.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final BCryptPasswordEncoder FALLBACK_PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final JwtService jwtService;
    private final AppUserRepository userRepo;
    private final AuthenticationManager authManager;
    private final AuthCookieFactory authCookieFactory;
    private final ContactVerificationService contactVerificationService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> signup(
            @Valid @RequestBody AuthSignupRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new ApiResponse<>(
                        true,
                        "Account created. Verify your email and phone to continue.",
                        contactVerificationService.register(request)
                )
        );
    }

    @PostMapping("/verify-contact")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyContact(
            @Valid @RequestBody ContactVerificationRequest request
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Contact verification completed.",
                        contactVerificationService.verify(request)
                )
        );
    }

    @PostMapping("/resend-contact-verification")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resendContactVerification(
            @Valid @RequestBody VerificationResendRequest request
    ) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Verification code sent.",
                        contactVerificationService.resend(request)
                )
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Object>> login(
            @RequestBody LoginRequest loginRequest,
            HttpServletResponse response
    ) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmailOrUsername(),
                            loginRequest.getPassword()
                    )
            );
            return buildLoginResponse(auth, response);
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "This account is disabled", null));
        } catch (AuthenticationException e) {
            Authentication fallbackAuth = authenticateAgainstStoredHash(loginRequest);
            if (fallbackAuth != null) {
                return buildLoginResponse(fallbackAuth, response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Invalid credentials", null));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, authCookieFactory.clearSessionCookie().toString());
        return ResponseEntity.ok(new ApiResponse<>(true, "Logged out", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Not authenticated", null));
        }

        AppUser user = findByEmailOrUsername(auth.getName());
        return ResponseEntity.ok(new ApiResponse<>(true, "OK", toUserDto(user)));
    }

    private UserDto toUserDto(AppUser user) {
        String formattedUsername = StringUtils.capitalize(user.getUsername().toLowerCase());
        return new UserDto(
                user.getId(),
                formattedUsername,
                user.getEmail(),
                user.getRole().name(),
                user.getPhoneNumber(),
                !Boolean.FALSE.equals(user.getEmailVerified()),
                !StringUtils.hasText(user.getPhoneNumber()) || !Boolean.FALSE.equals(user.getPhoneVerified()),
                user.isPlatformAccess(),
                user.isEnabled()
        );
    }

    private AppUser findByEmailOrUsername(String principal) {
        return userRepo.findByEmail(principal)
                .or(() -> userRepo.findByUsername(principal))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private ResponseEntity<ApiResponse<Object>> buildLoginResponse(
            Authentication auth,
            HttpServletResponse response
    ) {
        SecurityContextHolder.getContext().setAuthentication(auth);
        String principal = auth.getName();

        AppUser user = findByEmailOrUsername(principal);
        if (!contactVerificationService.isFullyVerified(user)) {
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(
                            false,
                            "Verify your email and phone to continue.",
                            contactVerificationService.buildVerificationPayload(user, Map.of())
                    ));
        }

        UserDto userDto = toUserDto(user);
        String jwt = jwtService.generateToken(principal);
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                authCookieFactory.createSessionCookie(jwt).toString()
        );
        LoginResponse loginResponse = new LoginResponse(jwt, userDto);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Login Successful",
                        loginResponse
                )
        );
    }

    private Authentication authenticateAgainstStoredHash(LoginRequest loginRequest) {
        String identifier = String.valueOf(loginRequest.getEmailOrUsername()).trim();
        if (!StringUtils.hasText(identifier)) {
            return null;
        }

        AppUser user = userRepo.findByEmail(identifier)
                .or(() -> userRepo.findByUsername(identifier))
                .orElse(null);

        if (user == null || !user.isEnabled()) {
            return null;
        }

        String storedPassword = normalizeStoredPassword(user.getPassword());
        if (!StringUtils.hasText(storedPassword)) {
            return null;
        }

        if (!FALLBACK_PASSWORD_ENCODER.matches(loginRequest.getPassword(), storedPassword)) {
            return null;
        }

        return new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                )
        );
    }

    private String normalizeStoredPassword(String storedPassword) {
        String normalized = String.valueOf(storedPassword).trim();
        String bcryptPrefix = "{bcrypt}";
        if (normalized.regionMatches(true, 0, bcryptPrefix, 0, bcryptPrefix.length())) {
            normalized = normalized.substring(bcryptPrefix.length()).trim();
        }
        return normalized;
    }
}
