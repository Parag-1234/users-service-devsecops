
package com.example.users.service;

import com.example.users.dto.CreateUserRequest;
import com.example.users.dto.UpdateUserEmailRequest;
import com.example.users.dto.UpdateUserRequest;
import com.example.users.entity.User;
import com.example.users.exception.DuplicateEmailException;
import com.example.users.exception.UserNotFoundException;
import com.example.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldCreateUserSuccessfully() {
        CreateUserRequest request = new CreateUserRequest(
                "Parag", "Parida", "PARAG@example.com"
        );

        when(userRepository.existsByEmailIgnoreCase("parag@example.com"))
                .thenReturn(false);

        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = userService.createUser(request);

        assertEquals("Parag", response.firstName());
        assertEquals("Parida", response.lastName());
        assertEquals("parag@example.com", response.email());

        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {
        CreateUserRequest request = new CreateUserRequest(
                "Parag", "Parida", "parag@example.com"
        );

        when(userRepository.existsByEmailIgnoreCase("parag@example.com"))
                .thenReturn(true);

        assertThrows(
                DuplicateEmailException.class,
                () -> userService.createUser(request)
        );

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void shouldReturnAllUsers() {
        User user1 = new User("Parag", "Parida", "parag@example.com");
        User user2 = new User("Amit", "Kumar", "amit@example.com");

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        var response = userService.getAllUsers();

        assertEquals(2, response.size());
        assertEquals("Parag", response.get(0).firstName());
        assertEquals("Amit", response.get(1).firstName());
    }

    @Test
    void shouldReturnUserById() {
        User user = new User("Parag", "Parida", "parag@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var response = userService.getUserById(1L);

        assertEquals("Parag", response.firstName());
        assertEquals("parag@example.com", response.email());
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserById(99L)
        );
    }

    @Test
    void shouldUpdateUserSuccessfully() {
        User user = new User("Parag", "Parida", "parag@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot(
                "parag.updated@example.com", 1L
        )).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserRequest request = new UpdateUserRequest(
                "Parag", "Updated", "parag.updated@example.com"
        );

        var response = userService.updateUser(1L, request);

        assertEquals("Updated", response.lastName());
        assertEquals("parag.updated@example.com", response.email());
    }

    @Test
    void shouldRejectUpdateWithAnotherUsersEmail() {
        User user = new User("Parag", "Parida", "parag@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot(
                "amit@example.com", 1L
        )).thenReturn(true);

        UpdateUserRequest request = new UpdateUserRequest(
                "Parag", "Parida", "amit@example.com"
        );

        assertThrows(
                DuplicateEmailException.class,
                () -> userService.updateUser(1L, request)
        );

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void shouldUpdateEmailSuccessfully() {
        User user = new User("Parag", "Parida", "parag@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot(
                "new@example.com", 1L
        )).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = userService.updateEmail(
                1L, new UpdateUserEmailRequest("new@example.com")
        );

        assertEquals("new@example.com", response.email());
    }

    @Test
    void shouldDeleteUserSuccessfully() {
        User user = new User("Parag", "Parida", "parag@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).delete(user);
    }
}
