package com.auditvault.auditvault;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class AuditvaultApplicationTest {

    @Test
    void main_runsSpringApplication() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            String[] args = {"--spring.main.web-application-type=none"};

            AuditvaultApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(AuditvaultApplication.class, args));
        }
    }
}
