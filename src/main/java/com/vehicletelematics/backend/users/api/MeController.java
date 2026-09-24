package com.vehicletelematics.backend.users.api;

import com.vehicletelematics.backend.users.api.dto.ChangePasswordRequest;
import com.vehicletelematics.backend.users.api.dto.UpdateMeRequest;
import com.vehicletelematics.backend.users.api.dto.UserResponse;
import com.vehicletelematics.backend.users.domain.User;
import com.vehicletelematics.backend.users.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;
    private final SecurityContextRepository securityContextRepository;

    public MeController(UserService userService, SecurityContextRepository securityContextRepository) {
        this.userService = userService;
        this.securityContextRepository = securityContextRepository;
    }

    @PatchMapping
    public UserResponse update(
            @Valid @RequestBody UpdateMeRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        User user = userService.updateOwnProfile(
                authentication.getName(),
                request.email(),
                request.firstName(),
                request.lastName());

        if (!user.getEmail().equals(authentication.getName())) {
            var updatedAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                    user.getEmail(), authentication.getCredentials(), authentication.getAuthorities());
            updatedAuthentication.setDetails(authentication.getDetails());
            var context = SecurityContextHolder.getContext();
            context.setAuthentication(updatedAuthentication);
            securityContextRepository.saveContext(context, servletRequest, servletResponse);
        }

        return UserResponse.from(user);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        userService.changePassword(
                authentication.getName(),
                request.currentPassword(),
                request.newPassword());
    }
}
