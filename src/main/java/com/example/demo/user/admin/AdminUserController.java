package com.example.demo.user.admin;

import com.example.demo.user.dto.UserDetailsDTO;
import com.example.demo.user.dto.UserListDTO;
import com.example.demo.user.model.UserRole;
import com.example.demo.user.model.UserStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin Users", description = "Admin endpoints for managing users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "List users",
            description = "Paginated and sortable by createdAt, email, username, lastName, status or id. The search "
                    + "matches the id (with or without #), email, username or full name, ignoring diacritics.")
    @ApiResponse(responseCode = "200", description = "One page of users")
    public ResponseEntity<Page<UserListDTO>> getUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction) {
        return ResponseEntity.ok(adminUserService.getUsers(role, status, search, page, size, sort, direction));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by id", description = "Returns user details by id")
    public ResponseEntity<UserDetailsDTO> getUserById(@PathVariable Long id) {
        UserDetailsDTO user = adminUserService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user account",
            description = "Erases the user's personal data (GDPR): deletes their reports and photos, anonymizes claims/challenges on other users' reports, and deactivates the account")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/block")
    @Operation(summary = "Block user (full)",
            description = "Blocks the account from logging in, hides its active listings and declines its pending claims. Resolves open reports against the user.")
    public ResponseEntity<Void> blockUser(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        adminUserService.blockUser(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/partial-block")
    @Operation(summary = "Partially block user",
            description = "Restricts the account: it can still browse, post lost reports and answer claims, but can no longer post found items or send verification questions")
    public ResponseEntity<Void> partialBlockUser(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        adminUserService.partialBlockUser(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unblock")
    @Operation(summary = "Unblock user", description = "Restores the account to active and dismisses open reports against it")
    public ResponseEntity<Void> unblockUser(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        adminUserService.unblockUser(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sessions/revoke")
    @Operation(summary = "Sign a user out everywhere",
            description = "Revokes every refresh token of the user. Access tokens already issued stay valid until they expire (15 minutes).")
    @ApiResponse(responseCode = "204", description = "Sessions revoked")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<Void> revokeSessions(@PathVariable Long id) {
        adminUserService.revokeSessions(id);
        return ResponseEntity.noContent().build();
    }
}
