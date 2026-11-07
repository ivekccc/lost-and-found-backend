package com.example.demo.user.dto;

import com.example.demo.shared.validation.MaxUtf8Bytes;
import com.example.demo.user.service.EmailAddresses;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
@Data
public class RegisterRequestDTO {
    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    @Size(min = 5, max = 100, message = "{validation.email.size}")
    private String email;

    @NotBlank(message = "{validation.username.required}")
    @Size(min = 3, max = 50, message = "{validation.username.size}")
    private String username;

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, max = 64, message = "{validation.password.size}")
    @MaxUtf8Bytes(72)
    private String password;

    @NotBlank(message = "{validation.firstName.required}")
    @Size(max = 100, message = "{validation.firstName.size}")
    private String firstName;

    @NotBlank(message = "{validation.lastName.required}")
    @Size(max = 100, message = "{validation.lastName.size}")
    private String lastName;

    @Size(max = 20, message = "{validation.phoneNumber.size}")
    private String phoneNumber;

    public void setEmail(String email) {
        this.email = EmailAddresses.normalize(email);
    }
}
