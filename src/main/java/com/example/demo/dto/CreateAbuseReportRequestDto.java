package com.example.demo.dto;

import com.example.demo.model.AbuseReason;
import com.example.demo.model.AbuseTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAbuseReportRequestDto {

    @NotNull(message = "Target type is required")
    private AbuseTargetType targetType;

    @NotNull(message = "Target id is required")
    private Long targetId;

    @NotNull(message = "Reason is required")
    private AbuseReason reason;

    @Size(max = 1000, message = "Message must be less than 1000 characters")
    private String message;
}
