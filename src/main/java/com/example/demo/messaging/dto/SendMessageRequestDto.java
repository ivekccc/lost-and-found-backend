package com.example.demo.messaging.dto;

import com.example.demo.messaging.model.MessageType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(name = "SendMessageRequestDto",
        description = "A TEXT message needs a body; a MEETING_POINT message needs latitude and longitude "
                + "inside the report's city. CONTACT is sent through the dedicated endpoint.")
public class SendMessageRequestDto {

    @NotNull(message = "{validation.chat.type.required}")
    private MessageType type;

    @Size(max = 2000, message = "{validation.chat.body.size}")
    private String body;

    @DecimalMin(value = "-90.0", message = "{validation.location.latitude.range}")
    @DecimalMax(value = "90.0", message = "{validation.location.latitude.range}")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "{validation.location.longitude.range}")
    @DecimalMax(value = "180.0", message = "{validation.location.longitude.range}")
    private BigDecimal longitude;

    @Size(max = 64, message = "{validation.chat.clientId.size}")
    private String clientId;
}
