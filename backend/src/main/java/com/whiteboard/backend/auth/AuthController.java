package com.whiteboard.backend.auth;

import com.whiteboard.backend.auth.dto.AuthResponse;
import com.whiteboard.backend.auth.dto.LoginDto;
import com.whiteboard.backend.auth.dto.RegisterDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping()
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody RegisterDto dto
    ) {
        return ResponseEntity.ok(
                authService.register(dto)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginDto dto
    ) {
        return ResponseEntity.ok(
                authService.login(dto)
        );
    }
}