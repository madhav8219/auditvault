package com.auditvault.auditvault.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping({"", "/"})
    public Map<String, String> home() {
        return Map.of(
            "message", "Welcome to AuditVault API",
            "status", "OK",
            "documentation", "http://localhost:8080/swagger-ui/index.html"
        );
    }
}
