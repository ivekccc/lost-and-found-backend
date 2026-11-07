package com.example.demo.messaging.controller;

import com.example.demo.messaging.dto.ConversationDetailsDto;
import com.example.demo.messaging.dto.ConversationDto;
import com.example.demo.messaging.dto.ConversationUnreadCountDto;
import com.example.demo.messaging.dto.MessageDto;
import com.example.demo.messaging.dto.MessagePageDto;
import com.example.demo.messaging.dto.SendMessageRequestDto;
import com.example.demo.messaging.dto.SendThanksRequestDto;
import com.example.demo.messaging.service.MessagingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "Chat between a finder and the owner after an approved claim")
public class ConversationController {

    private final MessagingService messagingService;

    @GetMapping
    @Operation(summary = "Get my conversations",
            description = "Conversations the caller takes part in, most recently active first. A conversation "
                    + "opens when a claim is approved, between the author of the questions and the claimant.")
    @ApiResponse(responseCode = "200", description = "Conversations returned",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ConversationDto.class))))
    public ResponseEntity<List<ConversationDto>> getConversations(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(messagingService.getConversations(userDetails.getUsername()));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count conversations with unread messages")
    @ApiResponse(responseCode = "200", description = "Count returned",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ConversationUnreadCountDto.class)))
    public ResponseEntity<ConversationUnreadCountDto> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(messagingService.getUnreadCount(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a conversation")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversation returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConversationDetailsDto.class))),
            @ApiResponse(responseCode = "404", description = "No such conversation of the caller")
    })
    public ResponseEntity<ConversationDetailsDto> getConversation(@PathVariable Long id,
                                                                  @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(messagingService.getConversation(id, userDetails.getUsername()));
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "Get messages",
            description = "Newest first. Pass the smallest id received as beforeId to load older messages.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Messages returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessagePageDto.class))),
            @ApiResponse(responseCode = "404", description = "No such conversation of the caller")
    })
    public ResponseEntity<MessagePageDto> getMessages(@PathVariable Long id,
                                                      @RequestParam(required = false) Long beforeId,
                                                      @RequestParam(required = false) Integer size,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(messagingService.getMessages(id, userDetails.getUsername(), beforeId, size));
    }

    @PostMapping("/{id}/messages")
    @Operation(summary = "Send a message",
            description = "Sends text or a proposed meeting point. The message is also pushed over the "
                    + "WebSocket to both participants, and as a push notification to the other one when "
                    + "they are not connected. At most 20 messages a minute.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Message sent",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageDto.class))),
            @ApiResponse(responseCode = "400", description = "Empty text, or meeting point outside the report's city"),
            @ApiResponse(responseCode = "404", description = "No such conversation of the caller"),
            @ApiResponse(responseCode = "429", description = "Too many messages")
    })
    public ResponseEntity<MessageDto> sendMessage(@PathVariable Long id,
                                                  @Valid @RequestBody SendMessageRequestDto request,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        MessageDto sent = messagingService.sendMessage(id, userDetails.getUsername(), request);
        return ResponseEntity.created(URI.create("/conversations/" + id + "/messages/" + sent.getId())).body(sent);
    }

    @PostMapping("/{id}/contact")
    @Operation(summary = "Share my contact",
            description = "Posts the caller's contact details as a message: the report's contact when the caller "
                    + "owns the report and set one, otherwise the account email and phone.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Contact shared",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageDto.class))),
            @ApiResponse(responseCode = "404", description = "No such conversation of the caller"),
            @ApiResponse(responseCode = "429", description = "Too many messages")
    })
    public ResponseEntity<MessageDto> shareContact(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserDetails userDetails) {
        MessageDto sent = messagingService.shareContact(id, userDetails.getUsername());
        return ResponseEntity.created(URI.create("/conversations/" + id + "/messages/" + sent.getId())).body(sent);
    }

    @PostMapping("/{id}/thanks")
    @Operation(summary = "Thank the finder",
            description = "Posts a thank-you from the owner of the item to the finder, with an optional note. "
                    + "Allowed once per conversation and only for the owner of the item; the finder also "
                    + "gets it in the notification inbox.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Thanks sent",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageDto.class))),
            @ApiResponse(responseCode = "400", description = "The caller is the finder, not the owner of the item"),
            @ApiResponse(responseCode = "404", description = "No such conversation of the caller"),
            @ApiResponse(responseCode = "409", description = "Thanks already sent in this conversation"),
            @ApiResponse(responseCode = "429", description = "Too many messages")
    })
    public ResponseEntity<MessageDto> sendThanks(@PathVariable Long id,
                                                 @Valid @RequestBody(required = false) SendThanksRequestDto request,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        MessageDto sent = messagingService.sendThanks(id, userDetails.getUsername(), request);
        return ResponseEntity.created(URI.create("/conversations/" + id + "/messages/" + sent.getId())).body(sent);
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark a conversation read", description = "Also tells the other participant over the WebSocket.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Marked read"),
            @ApiResponse(responseCode = "404", description = "No such conversation of the caller")
    })
    public ResponseEntity<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        messagingService.markRead(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
