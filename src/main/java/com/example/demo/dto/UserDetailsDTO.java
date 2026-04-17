package com.example.demo.dto;

import com.example.demo.model.UserRole;
import com.example.demo.model.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsDTO {

    @NotNull
    private Long id;

    @NotBlank
    private String email;

    @NotBlank
    private String username;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String phoneNumber;

    @NotNull
    private UserStatus status;

    @NotNull
    private UserRole role;

    @NotNull
    private LocalDateTime createdAt;
}
