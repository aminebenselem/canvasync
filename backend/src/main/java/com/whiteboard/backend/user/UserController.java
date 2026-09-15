package com.whiteboard.backend.user;

import com.whiteboard.backend.user.dto.UserDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;


@RestController()
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserDto getCurrentUser(@AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.parseLong(Objects.requireNonNull(jwt.getSubject()));

        return userService.getUser(userId);
    }
}
