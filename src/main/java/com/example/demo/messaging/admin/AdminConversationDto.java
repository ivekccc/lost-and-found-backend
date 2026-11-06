package com.example.demo.messaging.admin;

import com.example.demo.messaging.dto.MessageDto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AdminConversationDto", description = "Whole conversation for moderation, oldest message first.")
public class AdminConversationDto {

    @NotNull
    private Long id;

    @NotNull
    private Long reportId;

    @NotBlank
    private String reportTitle;

    @NotNull
    private Long holderId;

    @NotBlank
    private String holderName;

    @NotNull
    private Long claimantId;

    @NotBlank
    private String claimantName;

    @NotNull
    @ArraySchema(schema = @Schema(implementation = MessageDto.class))
    private List<MessageDto> messages;
}
