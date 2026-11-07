package com.example.demo.user.dto;

import com.example.demo.user.model.AppLanguage;

import com.example.demo.location.dto.CityDto;
import com.example.demo.location.model.City;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {

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

    private String avatarUrl;

    @NotNull
    private Boolean googleLinked;

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    private String role;

    @NotNull
    @Schema(description = "City the user is currently browsing. Scopes search, the nearby feed and "
            + "address lookup; does NOT scope the user's own reports, claims or notifications, so "
            + "switching cities never hides your own listing or a claim awaiting your decision.")
    private CityDto activeCity;

    @NotNull
    @Schema(description = "Language the user chose in the app. Push notifications, inbox entries and "
            + "emails are written in it, since those are sent without a request to take a language from.")
    private AppLanguage language;

    @NotNull
    @Schema(description = "How many thank-yous the user has received as a finder.")
    private Long thanksReceived;
}
