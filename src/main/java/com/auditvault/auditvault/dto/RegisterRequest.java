package com.auditvault.auditvault.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "RegisterRequest", description = "Registration payload for creating a new AuditVault user account")
public class RegisterRequest {
    @NotBlank(message = "username is required")
    @Size(min = 3, max = 50, message = "username length must be between 3 and 50 characters")
    @Schema(description = "Unique username for the account", example = "alice")
    private String username;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    @Schema(description = "User email address", example = "alice@example.com")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "password must be at least 8 characters")
    @Schema(description = "User password", example = "StrongPass!123")
    private String password;
}
