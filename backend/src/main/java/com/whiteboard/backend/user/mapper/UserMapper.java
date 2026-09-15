package com.whiteboard.backend.user.mapper;

import com.whiteboard.backend.user.User;
import com.whiteboard.backend.user.dto.UserDto;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );
    }
}
