package com.example.demo.user.dto;

import com.example.demo.user.service.EmailAddresses;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data
public class ForgotPasswordRequestDto {

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    public void setEmail(String email) {
        this.email = EmailAddresses.normalize(email);
    }
}
