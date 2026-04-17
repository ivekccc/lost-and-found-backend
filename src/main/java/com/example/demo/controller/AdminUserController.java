package com.example.demo.controller;

import com.example.demo.dto.UserListDTO;
import com.example.demo.model.UserRole;
import com.example.demo.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    @Operation(summary = "Get all users", description = "Returns a list of users, optionally filtered by role")
    public ResponseEntity<List<UserListDTO>> getAllUsers(@RequestParam(required = false) UserRole role) {
        List<UserListDTO> users = adminUserService.getAllUsers(role);
        return ResponseEntity.ok(users);
    }
}
