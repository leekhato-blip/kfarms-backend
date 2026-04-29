package com.kfarms.controller;

import com.kfarms.dto.LoginRequest;
import com.kfarms.dto.LoginResponse;
import com.kfarms.dto.UserDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.AppUser;
import com.kfarms.entity.Role;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.security.JwtCookie;
import com.kfarms.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final BCryptPasswordEncoder FALLBACK_PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final JwtService jwtService;
    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;

    public AuthController(AppUserRepository userRepo,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authManager,
                          JwtService jwtService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.authManager = authManager;
        this.jwtService = jwtService;
        System.out.println("✅ AuthController loaded!");
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signup(@RequestBody AppUser user){

        if(userRepo.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Email already registered", null));
        }

        if(userRepo.findByUsername(user.getUsername()).isPresent()){
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Username already taken", null));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.USER);

        userRepo.save(user);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "User registered successfully", null)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest,
                                                                HttpServletResponse response){
        try{
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
        } catch (AuthenticationException e){
            Authentication fallbackAuth = authenticateAgainstStoredHash(loginRequest);
            if (fallbackAuth != null) {
                return buildLoginResponse(fallbackAuth, response);
            }
            return ResponseEntity.status(401)
                    .body(new ApiResponse<>(false, "Invalid credentials", null));
        }
    }
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletResponse response) {
        boolean isProd = false;

        ResponseCookie cookie = ResponseCookie.from(JwtCookie.ACCESS_COOKIE, "")
                .httpOnly(true)
                .secure(isProd)
                .path(JwtCookie.PATH)
                .maxAge(0)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return  ResponseEntity.ok(new ApiResponse<>(true, "Logged out", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, "Not authenticated", null));
        }
        String principal = auth.getName();

        AppUser user = findByEmailOrUsername(principal);

        String formattedUsername = StringUtils.capitalize(user.getUsername().toLowerCase());

        UserDto userDto = new UserDto(
                user.getId(),
                formattedUsername,
                user.getEmail(),
                user.getRole().name()
        );

        return ResponseEntity.ok(new ApiResponse<>(true, "OK", userDto));
    }

    private AppUser findByEmailOrUsername(String principal) {
        return userRepo.findByEmail(principal)
                .or(() -> userRepo.findByUsername(principal))
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private ResponseEntity<ApiResponse<LoginResponse>> buildLoginResponse(
            Authentication auth,
            HttpServletResponse response
    ) {
        SecurityContextHolder.getContext().setAuthentication(auth);
        String principal = auth.getName();

        AppUser user = findByEmailOrUsername(principal);
        UserDto userDto = toUserDto(user);
        String jwt = jwtService.generateToken(principal);

        boolean isProd = false;
        ResponseCookie cookie = ResponseCookie.from(JwtCookie.ACCESS_COOKIE, jwt)
                .httpOnly(true)
                .secure(isProd)
                .path(JwtCookie.PATH)
                .maxAge(Duration.ofDays(1))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        LoginResponse loginResponse = new LoginResponse(jwt, userDto);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Login Successful",
                        loginResponse
                )
        );
    }

    private UserDto toUserDto(AppUser user) {
        String formattedUsername = StringUtils.capitalize(user.getUsername().toLowerCase());
        return new UserDto(
                user.getId(),
                formattedUsername,
                user.getEmail(),
                user.getRole().name()
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
