package com.auditvault.auditvault.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "AuthRequest", description = "Authentication payload for login")
public class AuthRequest {
    @NotBlank(message = "username is required")
    @Schema(description = "Registered username", example = "alice")
    private String username;

    @NotBlank(message = "password is required")
    @Schema(description = "Account password", example = "StrongPass!123")
    private String password;
}
