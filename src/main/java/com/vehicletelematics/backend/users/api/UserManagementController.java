package com.vehicletelematics.backend.users.api;

import com.vehicletelematics.backend.users.api.dto.CreateUserRequest;
import com.vehicletelematics.backend.users.api.dto.SetPasswordRequest;
import com.vehicletelematics.backend.users.api.dto.UpdateUserRequest;
import com.vehicletelematics.backend.users.api.dto.UserResponse;
import com.vehicletelematics.backend.users.domain.User;
import com.vehicletelematics.backend.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserService userService;

    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return userService.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable long id) {
        return UserResponse.from(userService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.create(
                request.email(),
                request.firstName(),
                request.lastName(),
                request.password(),
                request.role());
        return UserResponse.from(user);
    }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable long id, @Valid @RequestBody UpdateUserRequest request) {
        User user = userService.update(
                id,
                request.email(),
                request.firstName(),
                request.lastName(),
                request.role(),
                request.enabled());
        return UserResponse.from(user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable long id) {
        userService.disable(id);
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPassword(@PathVariable long id, @Valid @RequestBody SetPasswordRequest request) {
        userService.setPassword(id, request.newPassword());
    }
}
