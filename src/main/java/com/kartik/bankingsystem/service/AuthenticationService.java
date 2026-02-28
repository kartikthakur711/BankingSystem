package com.kartik.bankingsystem.service;

import com.kartik.bankingsystem.dto.AuthResponse;
import com.kartik.bankingsystem.dto.LoginRequest;
import com.kartik.bankingsystem.dto.RefreshTokenRequest;
import com.kartik.bankingsystem.dto.RegisterRequest;
import com.kartik.bankingsystem.entity.Role;
import com.kartik.bankingsystem.entity.User;
import com.kartik.bankingsystem.exception.UnauthorizedException;
import com.kartik.bankingsystem.repository.UserRepository;
import com.kartik.bankingsystem.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.jwt.access-expiration-ms}")
    private long accessExpirationMs;

    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of(Role.CUSTOMER))
                .active(true)
                .build();

        User saved = userRepository.save(user);
        UserDetails userDetails = userDetailsService.loadUserByUsername(saved.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.create(saved.getId()).getToken();

        return new AuthResponse(accessToken, refreshToken, "Bearer", accessExpirationMs / 1000);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
        );

        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.create(user.getId()).getToken();

        return new AuthResponse(accessToken, refreshToken, "Bearer", accessExpirationMs / 1000);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        var stored = refreshTokenService.verifyAndGet(request.refreshToken());

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found for refresh token"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);

        return new AuthResponse(accessToken, stored.getToken(), "Bearer", accessExpirationMs / 1000);
    }
}
