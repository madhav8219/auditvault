package com.auditvault.auditvault.security;

import com.auditvault.auditvault.domain.Role;
import com.auditvault.auditvault.domain.UserEntity;
import com.auditvault.auditvault.repository.RoleRepository;
import com.auditvault.auditvault.repository.UserRepository;
import com.auditvault.auditvault.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class JwtSecurityIntegrationTest {

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;

    private String baseUrl;
    private String username;
    private String password;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        baseUrl = "http://localhost:" + port;
        username = "user-" + UUID.randomUUID();
        password = "Pass!" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role adminRole = roleRepository.save(new Role(null, "ADMIN"));
        UserEntity user = UserEntity.builder()
            .username(username)
            .email(username + "@example.com")
            .password(passwordEncoder.encode(password))
            .enabled(true)
            .roles(Set.of(adminRole))
            .build();
        userRepository.save(user);
    }

    @Test
    void rootEndpointShouldBePublic() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Welcome to AuditVault API");
    }

    @Test
    void protectedEndpointShouldRequireAuthentication() {
        assertThatThrownBy(() -> restTemplate.getForEntity(baseUrl + "/audit/count", String.class))
            .isInstanceOf(HttpClientErrorException.class)
            .satisfies(ex -> assertThat(((HttpClientErrorException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void protectedEndpointShouldWorkWithValidJwt() {
        String token = jwtService.generateToken(username);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/audit/count", org.springframework.http.HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
