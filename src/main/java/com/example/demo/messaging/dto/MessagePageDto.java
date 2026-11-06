package com.example.demo.messaging.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MessagePageDto", description = "Messages newest first, with whether older ones exist.")
public class MessagePageDto {

    @NotNull
    @ArraySchema(schema = @Schema(implementation = MessageDto.class))
    private List<MessageDto> messages;

    @NotNull
    private Boolean hasMore;
}
