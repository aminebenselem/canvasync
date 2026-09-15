package com.whiteboard.backend.user;


import com.whiteboard.backend.user.dto.UserDto;
import com.whiteboard.backend.user.exception.UserNotFoundException;
import com.whiteboard.backend.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

     public UserDto getUser(Long id){
     User user= userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
     return  userMapper.toDto(user);
    }
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User getUserEntity(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
    }
    //uses already hashed password from the user object
    public User createUser(User user) {
    return userRepository.save(user);
    }
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    public boolean existsByUsername(String email) {
        return userRepository.existsByEmail(email);
    }




}
