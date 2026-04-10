package com.example.demo.controller;

import com.example.demo.dto.CloudinarySignatureDTO;
import com.example.demo.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cloudinary")
@RequiredArgsConstructor
@Tag(name = "Cloudinary", description = "Cloudinary upload signature endpoints")
public class CloudinaryController {

    private final CloudinaryService cloudinaryService;

    @GetMapping("/signature")
    @Operation(summary = "Get upload signature", description = "Generates a Cloudinary upload signature for direct client upload")
    @ApiResponse(responseCode = "200", description = "Signature generated successfully")
    public ResponseEntity<CloudinarySignatureDTO> getSignature() {
        return ResponseEntity.ok(cloudinaryService.getCloudinarySignature());
    }
}