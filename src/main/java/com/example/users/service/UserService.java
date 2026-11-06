package com.example.users.service;

import com.example.users.dto.CreateUserRequest;
import com.example.users.dto.UpdateUserEmailRequest;
import com.example.users.dto.UpdateUserRequest;
import com.example.users.dto.UserResponse;
import com.example.users.entity.User;
import com.example.users.exception.DuplicateEmailException;
import com.example.users.exception.UserNotFoundException;
import com.example.users.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse createUser(CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(email);
        }

        User user = new User(
                request.firstName().trim(),
                request.lastName().trim(),
                email
        );

        return UserResponse.from(saveUser(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return UserResponse.from(findUser(id));
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findUser(id);
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateEmailException(email);
        }

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);

        return UserResponse.from(saveUser(user));
    }

    public UserResponse updateEmail(Long id, UpdateUserEmailRequest request) {
        User user = findUser(id);
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateEmailException(email);
        }

        user.setEmail(email);
        return UserResponse.from(saveUser(user));
    }

    public void deleteUser(Long id) {
        User user = findUser(id);
        userRepository.delete(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private User saveUser(User user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            // Protects against a duplicate-email race between the check and database write.
            throw new DuplicateEmailException(user.getEmail());
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
