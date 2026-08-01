package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ChangeCityRequestDto", description = "Which city to browse from now on.")
public class ChangeCityRequestDto {

    @NotNull(message = "City is required")
    @Schema(description = "Id from GET /cities.")
    private Long cityId;
}
