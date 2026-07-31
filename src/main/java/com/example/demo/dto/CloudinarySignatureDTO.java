package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloudinarySignatureDTO {

    @NotBlank
    private String signature;

    @NotNull
    private Long timestamp;

    @NotBlank
    private String cloudName;

    @NotBlank
    private String apiKey;

    /**
     * Puna putanja pod kojom slika SME da se otpremi, npr. {@code lost-and-found/7/a3f2...}.
     * Server je generise i potpisuje, klijent je salje nepromenjenu kao {@code public_id}.
     * Vlasnistvo nad slikom time postaje deo njenog imena i moze se proveriti bez ikakvog
     * dodatnog zapisa — vidi CloudinaryService.getCloudinarySignature.
     */
    @NotBlank
    private String publicId;
}
