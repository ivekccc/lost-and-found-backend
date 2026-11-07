package com.example.demo.user.dto;

import com.example.demo.user.service.EmailAddresses;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class AuthRequestDTO {
    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(min = 5, max = 100, message = "{validation.email.size}")
    private String email;

    @NotBlank(message = "{validation.password.required}")
    @Size(max = 128, message = "{validation.password.tooLong}")
    private String password;

    public void setEmail(String email) {
        this.email = EmailAddresses.normalize(email);
    }
}
