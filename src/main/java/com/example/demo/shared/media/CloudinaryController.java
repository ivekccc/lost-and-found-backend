package com.example.demo.shared.media;

import com.example.demo.shared.exception.ErrorResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    @Operation(summary = "Get upload signature", description = "Generates a signature for ONE direct client upload. The client sends back public_id, allowed_formats, overwrite, transformation and timestamp exactly as returned; the signed public_id is namespaced to the caller, formats are limited to photos and the incoming transformation re-encodes the image, which strips EXIF and location metadata. Limited per user.")
    @ApiResponse(responseCode = "200", description = "Signature generated successfully")
    @ApiResponse(responseCode = "429", description = "Too many signatures requested",
            content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
    public ResponseEntity<CloudinarySignatureDTO> getSignature(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(cloudinaryService.getCloudinarySignature(userDetails.getUsername()));
    }
}
