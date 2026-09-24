package com.vehicletelematics.backend.users.bootstrap;

import com.vehicletelematics.backend.users.domain.UserRole;
import com.vehicletelematics.backend.users.repository.UserRepository;
import com.vehicletelematics.backend.users.service.UserService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class InitialAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminInitializer.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final boolean enabled;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String password;

    public InitialAdminInitializer(
            UserRepository userRepository,
            UserService userService,
            @Value("${app.initial-admin.enabled}") boolean enabled,
            @Value("${app.initial-admin.email}") String email,
            @Value("${app.initial-admin.first-name}") String firstName,
            @Value("${app.initial-admin.last-name}") String lastName,
            @Value("${app.initial-admin.password}") String password) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.enabled = enabled;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (!enabled || userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        userService.create(email, firstName, lastName, password, UserRole.ADMIN);
        log.warn("Created initial administrator '{}'. Change its configured password before deployment.", email);
    }
}
