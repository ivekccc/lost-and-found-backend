package com.example.demo.user.service;

import com.example.demo.shared.security.AuthenticatedUser;
import com.example.demo.user.model.User;
import com.example.demo.user.model.UserStatus;
import com.example.demo.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class MyUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(EmailAddresses.normalize(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toAuthenticatedUser(user);
    }

    public static AuthenticatedUser toAuthenticatedUser(User user) {
        return new AuthenticatedUser(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getRole().name(),
                user.getStatus() != UserStatus.BLOCKED && user.getStatus() != UserStatus.DELETED,
                user.getTokenVersion());
    }
}
