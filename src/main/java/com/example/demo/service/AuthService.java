package com.example.demo.service;

import com.example.demo.dto.AuthRequestDTO;
import com.example.demo.dto.RegisterRequestDto;
import com.example.demo.dto.AuthResponseDTO;
import com.example.demo.exception.UserAlreadyExistsException;
import com.example.demo.model.AuthProvider;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.dto.RefreshTokenRequestDTO;
import com.example.demo.dto.RefreshTokenResponseDTO;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public AuthService(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      JwtUtil jwtUtil,
                      AuthenticationManager authenticationManager,
                      UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    public AuthResponseDTO register(RegisterRequestDto req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Email already exists");
        }
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException("Username already exists");
        }

        User u = new User();
        u.setEmail(req.getEmail());
        u.setPassword(passwordEncoder.encode(req.getPassword()));
        u.setProvider(AuthProvider.LOCAL);
        u.setFirstName(req.getFirstName());
        u.setLastName(req.getLastName());
        u.setUsername(req.getUsername());
        u.setPhoneNumber(req.getPhoneNumber());
        userRepository.save(u);
        String token = jwtUtil.generateToken(u.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(u.getEmail());
        return new AuthResponseDTO(token, refreshToken, "User registered");
    }

    public AuthResponseDTO login(AuthRequestDTO req) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );
        String token = jwtUtil.generateToken(req.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(req.getEmail());
        return new AuthResponseDTO(token, refreshToken, "Login successful");
    }

    public RefreshTokenResponseDTO refreshToken(RefreshTokenRequestDTO req) {
        String refreshToken = req.getRefreshToken();
        String username = jwtUtil.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtUtil.validateRefreshToken(refreshToken, userDetails)) {
            return new RefreshTokenResponseDTO(null, null, "Invalid refresh token");
        }
        String newAccessToken = jwtUtil.generateToken(username);
        String newRefreshToken = jwtUtil.generateRefreshToken(username);
        return new RefreshTokenResponseDTO(newAccessToken, newRefreshToken, "Token refreshed");
    }
}
