package com.example.demo.messaging.dto;

import com.example.demo.messaging.model.ConversationRole;
import com.example.demo.report.model.ReportType;

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
@Schema(name = "ConversationDetailsDto", description = "Conversation header for one participant.")
public class ConversationDetailsDto {

    @NotNull
    private Long id;

    @NotNull
    private Long otherUserId;

    @NotBlank
    private String otherUserName;

    private String otherUserAvatarUrl;

    @NotNull
    private Long reportId;

    @NotBlank
    private String reportTitle;

    @NotNull
    private ReportType reportType;

    @NotNull
    private ConversationRole myRole;

    @Schema(description = "Until when the other participant has read the conversation.")
    private LocalDateTime otherReadAt;

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    @Schema(description = "True for the owner of the item while no thanks has been sent in this conversation, "
            + "when POST /conversations/{id}/thanks is offered.")
    private Boolean canThank;
}
