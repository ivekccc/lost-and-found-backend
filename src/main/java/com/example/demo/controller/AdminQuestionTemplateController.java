package com.example.demo.controller;

import com.example.demo.dto.AdminQuestionTemplateDto;
import com.example.demo.dto.CreateQuestionTemplateRequestDto;
import com.example.demo.dto.UpdateQuestionTemplateRequestDto;
import com.example.demo.service.AdminQuestionTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/admin/question-templates")
@RequiredArgsConstructor
@Tag(name = "Admin Question Templates", description = "Admin endpoints for managing verification question templates")
public class AdminQuestionTemplateController {

    private final AdminQuestionTemplateService adminQuestionTemplateService;

    @GetMapping
    @Operation(summary = "Get question templates", description = "Returns all templates (including inactive), optionally filtered by category")
    public ResponseEntity<List<AdminQuestionTemplateDto>> getTemplates(@RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(adminQuestionTemplateService.getTemplates(categoryId));
    }

    @PostMapping
    @Operation(summary = "Create question template", description = "Creates a new verification question template for a category")
    public ResponseEntity<AdminQuestionTemplateDto> createTemplate(
            @Valid @RequestBody CreateQuestionTemplateRequestDto request) {

        AdminQuestionTemplateDto created = adminQuestionTemplateService.createTemplate(request);
        URI location = URI.create("/admin/question-templates/" + created.getId());
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update question template", description = "Updates prompt, kind, choices and active flag. Existing challenges keep their copied questions.")
    public ResponseEntity<AdminQuestionTemplateDto> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuestionTemplateRequestDto request) {

        return ResponseEntity.ok(adminQuestionTemplateService.updateTemplate(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate question template", description = "Soft-deactivates the template so it is no longer offered when composing challenges")
    public ResponseEntity<Void> deactivateTemplate(@PathVariable Long id) {
        adminQuestionTemplateService.deactivateTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
