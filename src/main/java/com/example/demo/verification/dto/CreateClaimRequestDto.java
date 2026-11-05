package com.example.demo.verification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateClaimRequestDto {

    @Size(max = 1000, message = "{validation.message.size}")
    private String message;

    @Size(max = 500)
    private String photoUrl;

    @Size(max = 255)
    private String photoPublicId;

    @Valid
    @NotEmpty(message = "{validation.claim.answers.required}")
    private List<ClaimAnswerRequestDto> answers;
}
