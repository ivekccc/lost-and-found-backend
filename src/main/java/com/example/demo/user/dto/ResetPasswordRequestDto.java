package com.example.demo.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class ResetPasswordRequestDto {

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.code.required}")
    private String code;

    @NotBlank(message = "{validation.newPassword.required}")
    @Size(min = 6, max = 100, message = "{validation.password.size}")
    private String newPassword;
}
