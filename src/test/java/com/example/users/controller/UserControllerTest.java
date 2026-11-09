
package com.example.users.controller;

import com.example.users.dto.UserResponse;
import com.example.users.exception.DuplicateEmailException;
import com.example.users.exception.GlobalExceptionHandler;
import com.example.users.exception.UserNotFoundException;
import com.example.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private UserResponse sampleUser() {
        LocalDateTime now = LocalDateTime.now();

        return new UserResponse(
                1L,
                "Parag",
                "Parida",
                "parag@example.com",
                now,
                now
        );
    }

    @Test
    void shouldCreateUser() throws Exception {

        when(userService.createUser(any())).thenReturn(sampleUser());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "firstName": "Parag",
                                    "lastName": "Parida",
                                    "email": "parag@example.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Parag"))
                .andExpect(jsonPath("$.email").value("parag@example.com"));
    }

    @Test
    void shouldGetAllUsers() throws Exception {

        when(userService.getAllUsers())
                .thenReturn(List.of(sampleUser()));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].firstName").value("Parag"))
                .andExpect(jsonPath("$[0].email").value("parag@example.com"));
    }

    @Test
    void shouldGetUserById() throws Exception {

        when(userService.getUserById(1L))
                .thenReturn(sampleUser());

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Parag"));
    }

    @Test
    void shouldUpdateUser() throws Exception {

        when(userService.updateUser(eq(1L), any()))
                .thenReturn(sampleUser());

        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "firstName": "Parag",
                                    "lastName": "Parida",
                                    "email": "parag@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldUpdateEmail() throws Exception {

        when(userService.updateEmail(eq(1L), any()))
                .thenReturn(sampleUser());

        mockMvc.perform(patch("/api/users/1/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "parag@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("parag@example.com"));
    }

    @Test
    void shouldDeleteUser() throws Exception {

        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    @Test
    void shouldReturnBadRequestForInvalidEmail() throws Exception {

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "firstName": "Parag",
                                    "lastName": "Parida",
                                    "email": "invalid-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());

        verify(userService, never()).createUser(any());
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {

        when(userService.getUserById(99L))
                .thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturnConflictForDuplicateEmail() throws Exception {

        when(userService.createUser(any()))
                .thenThrow(new DuplicateEmailException("parag@example.com"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "firstName": "Parag",
                                    "lastName": "Parida",
                                    "email": "parag@example.com"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
