package com.example.users.controller;

import com.example.users.dto.CreateUserRequest;
import com.example.users.dto.UpdateUserEmailRequest;
import com.example.users.dto.UpdateUserRequest;
import com.example.users.dto.UserResponse;
import com.example.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.net.InetAddress;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.createUser(request));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/system-info")
    public ResponseEntity<String> getSystemInfo() {
        try {
            String ipAddress = InetAddress.getLocalHost().getHostAddress();

            String response = "Date: " + LocalDate.now()
                    + ", IP Address: " + ipAddress;

            return ResponseEntity.ok(response);

        } catch (Exception exception) {
            return ResponseEntity.internalServerError()
                    .body("Unable to retrieve server IP address");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @PatchMapping("/{id}/email")
    public ResponseEntity<UserResponse> updateEmail(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserEmailRequest request
    ) {
        return ResponseEntity.ok(userService.updateEmail(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
