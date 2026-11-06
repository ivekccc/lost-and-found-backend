package com.example.demo.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "NotificationSettingsDto",
        description = "Which kinds of notification arrive as a push. A disabled kind still lands in "
                + "the inbox; account notices always push.")
public class NotificationSettingsDto {

    @NotNull(message = "{validation.notificationSettings.required}")
    @Schema(description = "Possible matches for the user's own reports.")
    private Boolean matches;

    @NotNull(message = "{validation.notificationSettings.required}")
    @Schema(description = "New reports matching a saved search.")
    private Boolean savedSearches;

    @NotNull(message = "{validation.notificationSettings.required}")
    @Schema(description = "Verification questions and ownership claims.")
    private Boolean verification;

    @NotNull(message = "{validation.notificationSettings.required}")
    @Schema(description = "Own report expired or reunited.")
    private Boolean reportUpdates;
}
