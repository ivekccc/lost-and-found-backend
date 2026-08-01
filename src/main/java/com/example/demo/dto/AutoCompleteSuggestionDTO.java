package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Predlog adrese iz LocationIQ pretrage.
 *
 * {@code osmId} i {@code osmType} su obavezni jer se tim parom kreira oglas
 * ({@code LocationRequestDTO}). Bez {@code @NotBlank} generisani TypeScript tip ima oba
 * polja opciona, pa ih je svaki klijent morao kastovati da bi ih prosledio dalje — kast koji
 * bi tiho progutao predlog bez identiteta.
 */
@Data
@NoArgsConstructor
@Schema(description = "Address suggestion for report creation.")
public class AutoCompleteSuggestionDTO {

    @NotBlank
    private String osmId;

    @NotBlank
    private String osmType;

    @NotBlank
    private String displayName;

    // displayPlace i displayAddress su prikazni razbijeni oblik adrese i LocationIQ ih ne
    // vraca za svaki tip rezultata, pa ostaju opcioni.
    private String displayPlace;

    private String displayAddress;
}
