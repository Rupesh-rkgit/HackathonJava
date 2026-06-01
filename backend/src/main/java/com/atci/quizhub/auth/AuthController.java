package com.atci.quizhub.auth;

import com.atci.quizhub.user.User;
import com.atci.quizhub.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authManager, JwtService jwtService, UserRepository userRepository) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.enterpriseId(), req.password()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).build();
        }
        User u = userRepository.findByEnterpriseId(req.enterpriseId()).orElseThrow();
        String token = jwtService.generate(u.getEnterpriseId(), u.getRole().name());
        return ResponseEntity.ok(
            new LoginResponse(token, u.getRole().name(), u.getEnterpriseId(), u.getName()));
    }
}
