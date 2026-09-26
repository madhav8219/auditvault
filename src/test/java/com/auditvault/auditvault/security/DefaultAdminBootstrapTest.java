package com.auditvault.auditvault.security;

import com.auditvault.auditvault.dto.AuthRequest;
import com.auditvault.auditvault.dto.AuthResponse;
import com.auditvault.auditvault.dto.RegisterRequest;
import com.auditvault.auditvault.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DefaultAdminBootstrapTest {

    @Autowired
    private AuthService authService;

    @Test
    void userRegistrationAndLoginShouldWorkWithoutDefaultBootstrap() {
        String username = "user-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        String password = "TestPassword!" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        AuthResponse registration = authService.register(new RegisterRequest(username, username + "@example.com", password));
        assertThat(registration.getToken()).isNotBlank();

        AuthResponse login = authService.login(new AuthRequest(username, password));
        assertThat(login.getToken()).isNotBlank();
        assertThat(login.getUsername()).isEqualTo(username);
    }
}
