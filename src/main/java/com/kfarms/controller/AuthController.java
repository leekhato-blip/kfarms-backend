package com.kfarms.controller;

import com.kfarms.dto.LoginResponse;
import com.kfarms.entity.AppUser;
import com.kfarms.repository.UserRepository;
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
@RequestMapping("/api") // All routes under /api/auth
public class AuthController {
    private final JwtService jwtService;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;

    // Inject UserRepository and PasswordEncoder through constructor
    public AuthController(UserRepository userRepo, PasswordEncoder passwordEncoder, AuthenticationManager authManager, JwtService jwtService){
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.authManager = authManager;
        this.jwtService = jwtService;
        System.out.println("✅ AuthController loaded!");
    }

    // === REGISTER NEW USER === //
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody AppUser user){
        // Check if username already exists
        if(userRepo.findByUsername(user.getUsername()).isPresent()){
            return ResponseEntity.badRequest().body("Username already taken.");
        }

        // Encode the password using BCrypt
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Set a default role (can be changed as needed)
        user.setRole("ADMIN");

        // Save the new user in the database
        userRepo.save(user);

        return ResponseEntity.ok("User Registered Successfully");
    }

    // == Login Existing User == //
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody AppUser loginRequest){
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
            return ResponseEntity.ok("Login Successful" + new LoginResponse(jwt, loginRequest.getUsername()));
        }     catch (AuthenticationException e){
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }
}
