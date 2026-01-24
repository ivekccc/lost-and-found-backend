package com.example.demo.controller;

import com.example.demo.dto.AuthResponseDTO;
import com.example.demo.model.AuthProvider;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication endpoints")
public class GoogleAuthController {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @PostMapping("/google")
    public ResponseEntity<AuthResponseDTO> googleAuth(@RequestBody Map<String, String> body) {
        String idToken = body.get("credential");
        // Verifikuj Google token
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        @SuppressWarnings("unchecked")
        Map<String, Object> googleUser = restTemplate.getForObject(url, Map.class);
        if (googleUser == null || googleUser.get("email") == null) {
            return ResponseEntity.badRequest().body(new AuthResponseDTO(null, null, "Invalid Google token"));
        }
        String email = (String) googleUser.get("email");
        String googleId = (String) googleUser.get("sub");
        String firstName = (String) googleUser.get("given_name");
        String lastName = (String) googleUser.get("family_name");
        Optional<User> userOpt = userRepository.findByEmail(email);
        User user = userOpt.orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setCreatedAt(new Date());
            newUser.setProvider(AuthProvider.GOOGLE);
            newUser.setGoogleId(googleId);
            newUser.setUsername(email);
            return userRepository.save(newUser);
        });
        String token = jwtUtil.generateToken(user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
        return ResponseEntity.ok(new AuthResponseDTO(token, refreshToken, "Google login successful"));
    }
}
