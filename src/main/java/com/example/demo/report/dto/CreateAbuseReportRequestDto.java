package com.example.demo.report.dto;

import com.example.demo.report.model.AbuseReason;
import com.example.demo.report.model.AbuseTargetType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAbuseReportRequestDto {

    @NotNull(message = "{validation.abuse.targetType.required}")
    private AbuseTargetType targetType;

    @NotNull(message = "{validation.abuse.targetId.required}")
    private Long targetId;

    @NotNull(message = "{validation.abuse.reason.required}")
    private AbuseReason reason;

    @Size(max = 1000, message = "{validation.message.size}")
    private String message;
}
