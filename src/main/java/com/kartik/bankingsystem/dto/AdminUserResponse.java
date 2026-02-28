package com.kartik.bankingsystem.dto;

import com.kartik.bankingsystem.entity.Role;

import java.util.Set;

public record AdminUserResponse(
        String id,
        String fullName,
        String email,
        Set<Role> roles,
        boolean active
) {
}
