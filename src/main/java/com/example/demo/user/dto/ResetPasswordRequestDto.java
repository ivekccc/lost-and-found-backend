package com.example.demo.user.dto;

import com.example.demo.shared.validation.MaxUtf8Bytes;
import com.example.demo.user.service.EmailAddresses;

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
    @Size(min = 8, max = 64, message = "{validation.password.size}")
    @MaxUtf8Bytes(72)
    private String newPassword;

    public void setEmail(String email) {
        this.email = EmailAddresses.normalize(email);
    }
}
