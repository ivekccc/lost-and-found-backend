package com.example.demo.user.dto;

import com.example.demo.user.service.EmailAddresses;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class VerifyRequestDTO {

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    @NotBlank(message = "{validation.verificationCode.required}")
    private String code;

    @NotBlank(message = "{validation.password.required}")
    @Size(max = 128, message = "{validation.password.size}")
    private String password;

    public void setEmail(String email) {
        this.email = EmailAddresses.normalize(email);
    }
}
