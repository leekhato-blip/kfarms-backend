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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth") // All routes under /api/auth
public class AuthController {
    private final JwtService jwtService;
    private final AppUserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;

    // Inject AppUserRepository and PasswordEncoder through constructor
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

    // === REGISTER NEW USER === //
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signup(@RequestBody AppUser user){

        // Check if email already exists
        if(userRepo.findByEmail(user.getEmail()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Email already registered", null));
        }

        // Check if username already exists
        if(userRepo.findByUsername(user.getUsername()).isPresent()){
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Username already taken", null));
        }

        // Encode the password using BCrypt
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.ADMIN); // default role

        // Save the new user in the database
        userRepo.save(user);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "User registered successfully", null)
        );
    }

    // == Login Existing User (by email or username) == //
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest,
                                                                HttpServletResponse response){
        try{
            // Authenticate using email or username
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmailOrUsername(),
                            loginRequest.getPassword()
                    )
            );
 
            SecurityContextHolder.getContext().setAuthentication(auth);
            String principal = auth.getName(); // returns email from CustomUserDetailsService

            // =========================================
            // FETCH THE USER FROM THE DATABASE
            // =========================================
            AppUser user = userRepo.findByEmail(principal)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String formattedUsername = StringUtils.capitalize(user.getUsername().toLowerCase());
            UserDto userDto = new UserDto(
                    user.getId(),
                    formattedUsername,
                    user.getEmail(),
                    user.getRole().name()
            );
            // Generate token
            String jwt = jwtService.generateToken(principal);

            // Set HttpOnly cookie (dev-friendly: Secure=false on localhost)
            boolean isProd = false;
            ResponseCookie cookie = ResponseCookie.from(JwtCookie.ACCESS_COOKIE, jwt)
                    .httpOnly(true)
                    .secure(isProd) // true in production HTTPS
                    .path(JwtCookie.PATH)
                    .maxAge(Duration.ofDays(1))
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            // Optional: can stop returning jwt in body. For now keep it or set it null.
            LoginResponse loginResponse = new LoginResponse(jwt, userDto);

            return ResponseEntity.ok(
                    new ApiResponse<>(
                            true,
                            "Login Successful",
                            loginResponse
                    )
            );
        }     catch (AuthenticationException e){
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
                .maxAge(0)      // delete cookie
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
        String principal = auth.getName(); // email from CustomUserDetailsService

        AppUser user = userRepo.findByEmail(principal)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String formattedUsername = StringUtils.capitalize(user.getUsername().toLowerCase());

        UserDto userDto = new UserDto(
                user.getId(),
                formattedUsername,
                user.getEmail(),
                user.getRole().name()
        );

        return ResponseEntity.ok(new ApiResponse<>(true, "OK", userDto));
    }
}
