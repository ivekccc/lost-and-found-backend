package com.example.demo.shared.media;

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
     * The full path the image may be uploaded under, e.g. lost-and-found/7/a3f2... The server
     * generates and signs it and the client sends it back unchanged as public_id, so ownership
     * becomes part of the name and needs no separate record.
     */
    @NotBlank
    private String publicId;
}
