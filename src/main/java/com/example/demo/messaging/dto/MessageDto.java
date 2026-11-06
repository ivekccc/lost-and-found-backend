package com.example.demo.messaging.dto;

import com.example.demo.messaging.model.MessageType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MessageDto", description = "One message in a conversation. Which optional fields are set depends on the type.")
public class MessageDto {

    @NotNull
    private Long id;

    @NotNull
    private Long conversationId;

    @NotNull
    private Long senderId;

    @NotNull
    private MessageType type;

    @Schema(description = "Text of a TEXT message.")
    private String body;

    @Schema(description = "Point of a MEETING_POINT message.")
    private BigDecimal latitude;

    private BigDecimal longitude;

    @Schema(description = "Readable description of the meeting point.")
    private String placeLabel;

    @Schema(description = "Contact details shared in a CONTACT message, as they were when shared.")
    private String contactName;

    private String contactEmail;

    private String contactPhone;

    @Schema(description = "Echo of the client id sent with the message, for matching an optimistic copy.")
    private String clientId;

    @NotNull
    private LocalDateTime createdAt;
}
