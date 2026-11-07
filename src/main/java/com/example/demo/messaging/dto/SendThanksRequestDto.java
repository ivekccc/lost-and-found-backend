package com.example.demo.messaging.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name = "SendThanksRequestDto", description = "A thank-you from the owner of the item to the finder, "
        + "with an optional personal note.")
public class SendThanksRequestDto {

    @Size(max = 500, message = "{validation.chat.thanksNote.size}")
    private String note;
}
