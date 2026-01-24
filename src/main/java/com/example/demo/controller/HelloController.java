package com.example.demo.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Test", description = "Test endpoints")
public class HelloController {
    @GetMapping("/secret")
    public String secret() {
        return "Ovo je tajni string!";
    }

}
