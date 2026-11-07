package com.example.demo.messaging.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole(\'ADMIN\')")
@RestController
@RequestMapping("/admin/conversations")
@RequiredArgsConstructor
@Tag(name = "Admin conversations", description = "Reading conversations behind abuse reports")
public class AdminConversationController {

    private final AdminConversationService adminConversationService;

    @GetMapping("/{id}")
    @Operation(summary = "Get a conversation for moderation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversation returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AdminConversationDto.class))),
            @ApiResponse(responseCode = "404", description = "No such conversation")
    })
    public ResponseEntity<AdminConversationDto> getConversation(@PathVariable Long id) {
        return ResponseEntity.ok(adminConversationService.getConversation(id));
    }
}
