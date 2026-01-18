package com.kfarms.controller;

import com.kfarms.dto.LoginRequest;
import com.kfarms.dto.LoginResponse;
import com.kfarms.dto.UserDto;
import com.kfarms.entity.ApiResponse;
import com.kfarms.entity.AppUser;
import com.kfarms.entity.Role;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest){
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
}
