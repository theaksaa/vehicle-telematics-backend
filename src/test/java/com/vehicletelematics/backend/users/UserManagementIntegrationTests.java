package com.vehicletelematics.backend.users;

import com.vehicletelematics.backend.users.domain.User;
import com.vehicletelematics.backend.users.domain.UserRole;
import com.vehicletelematics.backend.users.repository.UserRepository;
import com.vehicletelematics.backend.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserManagementIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void adminCanManageUsersAndDeleteOnlyDisablesThem() throws Exception {
        String responseBody = mockMvc.perform(post("/api/users")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "New.User@Example.com",
                                  "firstName": "New",
                                  "lastName": "User",
                                  "password": "initial-password",
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.user@example.com"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(responseBody).contains("new.user@example.com");
        User created = userRepository.findByEmailIgnoreCase("new.user@example.com").orElseThrow();

        mockMvc.perform(get("/api/users/{id}", created.getId())
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("New"));

        mockMvc.perform(get("/api/users")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'new.user@example.com')]").exists());

        mockMvc.perform(patch("/api/users/{id}", created.getId())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Updated",
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(put("/api/users/{id}/password", created.getId())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"newPassword": "reset-password"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/users/{id}", created.getId())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        User disabled = userRepository.findById(created.getId()).orElseThrow();
        assertThat(disabled.isEnabled()).isFalse();
        assertThat(passwordEncoder.matches("reset-password", disabled.getPasswordHash())).isTrue();
    }

    @Test
    void userManagementRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users")
                        .with(user("user@example.com").roles("USER"))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "blocked@example.com",
                                  "firstName": "Blocked",
                                  "lastName": "User",
                                  "password": "blocked-password",
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isForbidden());

        assertThat(userRepository.existsByEmailIgnoreCase("blocked@example.com")).isFalse();
    }

    @Test
    void missingUserReturnsNotFoundAndDuplicateEmailReturnsConflict() throws Exception {
        User existing = userService.create(
                "existing@example.com", "Existing", "User", "valid-password", UserRole.USER);

        mockMvc.perform(get("/api/users/{id}", Long.MAX_VALUE)
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/users/{id}", existing.getId())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email": "admin@example.com"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void userCanUpdateOwnProfileAndEmailRemainsAuthenticatedInSameSession() throws Exception {
        userService.create("self@example.com", "Self", "User", "current-password", UserRole.USER);
        MockHttpSession session = login("self@example.com", "current-password");

        mockMvc.perform(patch("/api/me")
                        .session(session)
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "renamed@example.com",
                                  "firstName": "Renamed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("renamed@example.com"))
                .andExpect(jsonPath("$.firstName").value("Renamed"))
                .andExpect(jsonPath("$.role").value("USER"));

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("renamed@example.com"));
    }

    @Test
    void userCanChangeOwnPasswordOnlyWithCurrentPassword() throws Exception {
        userService.create("password@example.com", "Password", "User", "current-password", UserRole.USER);
        MockHttpSession session = login("password@example.com", "current-password");

        mockMvc.perform(put("/api/me/password")
                        .session(session)
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "wrong-password",
                                  "newPassword": "updated-password"
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/me/password")
                        .session(session)
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "current-password",
                                  "newPassword": "updated-password"
                                }
                                """))
                .andExpect(status().isNoContent());

        User updated = userRepository.findByEmailIgnoreCase("password@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("updated-password", updated.getPasswordHash())).isTrue();
    }

    private MockHttpSession login(String email, String password) throws Exception {
        return (MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession(false);
    }
}
