package com.whiteboard.backend.auth;

import com.whiteboard.backend.auth.dto.AuthResponse;
import com.whiteboard.backend.auth.dto.LoginDto;
import com.whiteboard.backend.auth.dto.RegisterDto;
import com.whiteboard.backend.auth.exception.EmailAlreadyExistsException;
import com.whiteboard.backend.auth.exception.InvalidCredentialsException;
import com.whiteboard.backend.auth.exception.UsernameAlreadyExistsException;
import com.whiteboard.backend.user.User;
import com.whiteboard.backend.user.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserService userService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterDto dto) {

        if (userService.existsByEmail(dto.email())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        if (userService.existsByUsername(dto.username())) {
            throw new UsernameAlreadyExistsException("Username already exists");
        }

        User user = new User();

        user.setUsername(dto.username());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.password()));

        User savedUser = userService.createUser(user);

        String token = jwtService.generateToken(savedUser);

        return new AuthResponse(token);
    }

    public AuthResponse login(LoginDto dto) {

        User user = userService.getUserByEmail(dto.email()).orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(
                dto.password(),
                user.getPassword()
        )) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        return new AuthResponse(
                jwtService.generateToken(user)
        );
    }
}