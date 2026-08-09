package com.techaxis.taskapi.service;

import com.techaxis.taskapi.dto.UserRequest;
import com.techaxis.taskapi.dto.UserResponse;
import com.techaxis.taskapi.entity.User;
import com.techaxis.taskapi.exception.DuplicateResourceException;
import com.techaxis.taskapi.exception.ResourceNotFoundException;
import com.techaxis.taskapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("A user with email '" + request.email() + "' already exists");
        }
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .build();
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    public UserResponse update(Long id, UserRequest request) {
        User user = getOrThrow(id);

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("A user with email '" + request.email() + "' already exists");
        }

        user.setName(request.name());
        user.setEmail(request.email());
        return toResponse(user);
    }

    public void delete(Long id) {
        User user = getOrThrow(id);
        userRepository.delete(user);
    }

    User getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
