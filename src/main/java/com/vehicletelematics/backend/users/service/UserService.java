package com.vehicletelematics.backend.users.service;

import com.vehicletelematics.backend.users.domain.User;
import com.vehicletelematics.backend.users.domain.UserRole;
import com.vehicletelematics.backend.users.exception.EmailAlreadyExistsException;
import com.vehicletelematics.backend.users.exception.InvalidCurrentPasswordException;
import com.vehicletelematics.backend.users.exception.UserNotFoundException;
import com.vehicletelematics.backend.users.repository.UserRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        User user = findByEmail(email);
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                .disabled(!user.isEnabled())
                .build();
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Transactional
    public User create(String email, String firstName, String lastName, String rawPassword, UserRole role) {
        String normalizedEmail = normalize(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }
        return userRepository.save(new User(
                normalizedEmail,
                firstName.trim(),
                lastName.trim(),
                passwordEncoder.encode(rawPassword),
                role));
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public User findById(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Transactional
    public User update(
            long id,
            String email,
            String firstName,
            String lastName,
            UserRole role,
            Boolean enabled) {
        User user = findById(id);
        String updatedEmail = email == null ? user.getEmail() : normalize(email);
        ensureEmailAvailable(updatedEmail, user.getId());
        user.updateProfile(
                updatedEmail,
                firstName == null ? user.getFirstName() : firstName.trim(),
                lastName == null ? user.getLastName() : lastName.trim());
        user.updateAdministration(
                role == null ? user.getRole() : role,
                enabled == null ? user.isEnabled() : enabled);
        return user;
    }

    @Transactional
    public User updateOwnProfile(String currentEmail, String email, String firstName, String lastName) {
        User user = findByEmail(currentEmail);
        String updatedEmail = email == null ? user.getEmail() : normalize(email);
        ensureEmailAvailable(updatedEmail, user.getId());
        user.updateProfile(
                updatedEmail,
                firstName == null ? user.getFirstName() : firstName.trim(),
                lastName == null ? user.getLastName() : lastName.trim());
        return user;
    }

    @Transactional
    public void disable(long id) {
        findById(id).disable();
    }

    @Transactional
    public void setPassword(long id, String newPassword) {
        findById(id).changePasswordHash(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }
        user.changePasswordHash(passwordEncoder.encode(newPassword));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void ensureEmailAvailable(String email, Long currentUserId) {
        userRepository.findByEmailIgnoreCase(email)
                .filter(existing -> !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new EmailAlreadyExistsException(email);
                });
    }
}
