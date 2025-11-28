package com.xypha.onlineBus.auth.controller;

import com.xypha.onlineBus.account.users.dto.UserRequest;
import com.xypha.onlineBus.account.users.dto.UserResponse;
import com.xypha.onlineBus.account.users.entity.User;
import com.xypha.onlineBus.account.users.service.CustomUserDetails;
import com.xypha.onlineBus.account.users.service.UserService;
import com.xypha.onlineBus.auth.dto.AuthRequest;
import com.xypha.onlineBus.auth.service.JwtService;
import com.xypha.onlineBus.restPassword.Service.AuthService;
import com.xypha.onlineBus.restPassword.entity.RestToken;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:63342")
public class AuthController {

    @Autowired
    private UserService userService;
    @Autowired
    private AuthService authService;

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController (AuthenticationManager authenticationManager, JwtService jwtService){
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping ("/login")
    public ResponseEntity<?> login(
            @RequestBody AuthRequest authRequest
            ) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()
                    )
            );

            String token = jwtService.generateToken(auth.getName(), auth.getAuthorities());
            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }
    }

    @PostMapping ("/register")
    public ResponseEntity<?> registerUser (
            @Valid @RequestBody UserRequest userRequest
            ){
        UserResponse savedUser = userService.createUser(userRequest);

        SimpleGrantedAuthority roleAuthority = new SimpleGrantedAuthority("ROLE_" + savedUser.getRole());
        String token = jwtService.generateToken(savedUser.getUsername(),
                List.of(roleAuthority));
        return ResponseEntity.ok(
                Map.of(
                        "User",savedUser,
                        "token",token
                )
        );
    }


    @PostMapping("/debug-login")
    public ResponseEntity<?> debugLogin(@RequestBody AuthRequest req) {
        try{
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            req.getUsername(),
                            req.getPassword()
                    )
                    );
            return ResponseEntity.ok(Map.of("authenticated",auth.isAuthenticated()));

        }catch (Exception e){
            return ResponseEntity.status(401)
                    .body(Map.of("error","Invalid Username or password"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getProfile (
            @AuthenticationPrincipal CustomUserDetails userDetails){
        if (userDetails == null){
            return ResponseEntity.status(401).body(Map.of("Error","Unauthorized"));

        }
        UserResponse user = userService.getUserById(userDetails.getId());
        return ResponseEntity.ok(user);
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgot (
            @RequestBody Map<String, String>request
    ) throws MessagingException {
        String email = request.get("email");
        return authService.forgotPassword(email);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> reset(
            @RequestParam String token,
            @RequestParam String newPassword
    ){
        return authService.resetPassword(token, newPassword);
    }

    @GetMapping("/reset-tokens")
    public ResponseEntity<?> getAllResetTokens(){
        List<RestToken> tokens = authService.getAllResetToken();
        return ResponseEntity.ok(tokens);
    }

}
