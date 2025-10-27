package com.kfarms.controller;

import com.kfarms.dto.LoginResponse;
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
        // Check if username already exists
        if(userRepo.findByUsername(user.getUsername()).isPresent()){
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Username already taken", null));
        }

        // Encode the password using BCrypt
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(Role.STAFF); // default role

        // Save the new user in the database
        userRepo.save(user);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "User registered successfully", null)
        );
    }

    // == Login Existing User == //
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody AppUser loginRequest){
        try{
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(auth);
            String username = auth.getName();
            String jwt = jwtService.generateToken(username);

            return ResponseEntity.ok(
                    new ApiResponse<>(
                            true,
                            "Login Successful",
                            new LoginResponse(jwt, username)
                    )
            );
        }     catch (AuthenticationException e){
            return ResponseEntity.status(401)
                    .body(new ApiResponse<>(false, "Invalid credentials", null));
        }
    }
}
