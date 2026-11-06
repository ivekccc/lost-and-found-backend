package com.example.demo.report.controller;

import com.example.demo.report.service.ReportShareService;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Hidden
@Controller
@RequiredArgsConstructor
public class ShareController {

    private final ReportShareService reportShareService;

    @GetMapping("/o/{token}")
    public String sharedReport(@PathVariable String token, Model model, HttpServletResponse response) {
        return reportShareService.describe(token)
                .map(view -> {
                    model.addAttribute("report", view);
                    return "share/report";
                })
                .orElseGet(() -> {
                    response.setStatus(HttpStatus.NOT_FOUND.value());
                    model.addAttribute("playStoreUrl", reportShareService.playStoreUrl());
                    return "share/unavailable";
                });
    }
}
