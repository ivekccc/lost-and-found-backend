package com.example.demo.controller;

import com.example.demo.dto.ChangeCityRequestDto;
import com.example.demo.dto.DeleteAccountRequestDto;
import com.example.demo.dto.UpdateUserProfileDTO;
import com.example.demo.dto.UserProfileDTO;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AccountDeletionService;
import com.example.demo.service.CityService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.*;

;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile endpoints")
public class UserController {

    private final UserRepository userRepository;
    private final AccountDeletionService accountDeletionService;
    private final CityService cityService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserProfileDTO.class)
                    )
            )
    })
    public ResponseEntity<UserProfileDTO> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(toProfileDto(user));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User profile updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = UserProfileDTO.class)
                    )
            )
    })
    public ResponseEntity<UserProfileDTO> updateProfile(
            @Valid @RequestBody UpdateUserProfileDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        userRepository.save(user);

        return ResponseEntity.ok(toProfileDto(user));
    }

    @PutMapping("/me/city")
    @Operation(summary = "Change the city being browsed",
            description = "Scopes search, the nearby feed, address lookup and matching to the given "
                    + "city. Own reports, claims and notifications are deliberately NOT scoped, so "
                    + "switching cities never hides your own listing or a claim awaiting your "
                    + "decision. Clients should refresh location-dependent screens afterwards.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "City changed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserProfileDTO.class))),
            @ApiResponse(responseCode = "400", description = "City is not available yet"),
            @ApiResponse(responseCode = "404", description = "No such city")
    })
    public ResponseEntity<UserProfileDTO> changeCity(
            @Valid @RequestBody ChangeCityRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {
        cityService.setActiveCity(userDetails.getUsername(), request.getCityId());

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(toProfileDto(user));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete current account",
            description = "Permanently erases the user's personal data (GDPR). Requires the current password, or a fresh Google ID token for Google-linked accounts. Own reports and their photos are deleted; claims/challenges on other users' reports are kept but anonymized.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Account deleted"),
            @ApiResponse(responseCode = "401", description = "Wrong password or Google confirmation")
    })
    public ResponseEntity<Void> deleteAccount(
            @Valid @RequestBody DeleteAccountRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails) {
        accountDeletionService.deleteOwnAccount(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }

    private UserProfileDTO toProfileDto(User user) {
        return new UserProfileDTO(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.getGoogleSub() != null,
                user.getCreatedAt(),
                user.getRole().name(),
                CityService.toDto(user.getActiveCity())
        );
    }
}
