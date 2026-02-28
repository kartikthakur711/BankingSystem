package com.kartik.bankingsystem.service;

import com.kartik.bankingsystem.dto.UserProfileResponse;
import com.kartik.bankingsystem.exception.UnauthorizedException;
import com.kartik.bankingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse getProfile(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRoles(),
                user.isActive()
        );
    }
}
