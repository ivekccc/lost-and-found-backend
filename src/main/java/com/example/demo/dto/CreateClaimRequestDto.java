package com.example.demo.dto;

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

    @Size(max = 1000, message = "Message must be less than 1000 characters")
    private String message;

    @Size(max = 500)
    private String photoUrl;

    @Size(max = 255)
    private String photoPublicId;

    @Valid
    @NotEmpty(message = "Answers are required")
    private List<ClaimAnswerRequestDto> answers;
}
