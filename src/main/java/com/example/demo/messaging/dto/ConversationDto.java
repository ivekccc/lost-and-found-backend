package com.example.demo.messaging.dto;

import com.example.demo.messaging.model.MessageType;
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
@Schema(name = "ConversationDto", description = "Conversation as listed for one participant.")
public class ConversationDto {

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

    private MessageType lastMessageType;

    @Schema(description = "Body of the last message when it is text.")
    private String lastMessageBody;

    private Boolean lastMessageMine;

    private LocalDateTime lastMessageAt;

    @NotNull
    private Integer unreadCount;

    @NotNull
    private LocalDateTime createdAt;
}
