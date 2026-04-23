package com.plgdhd.authservice.controller;

import com.plgdhd.authservice.dto.request.BanUserRequest;
import com.plgdhd.authservice.dto.request.ChangeRoleRequest;
import com.plgdhd.authservice.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
@Tag(name = "Admin Controller")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/{userId}/ban")
    @Operation(summary = "Ban a user")
    public ResponseEntity<Void> banUser(@PathVariable String userId,
                                        @Valid @RequestBody BanUserRequest request,
                                        @AuthenticationPrincipal Jwt jwt) {
        adminService.banUser(userId, jwt.getSubject(), request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/role")
    @Operation(summary = "Change user role")
    public ResponseEntity<Void> changeRole(@PathVariable String userId,
                                           @Valid @RequestBody ChangeRoleRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        adminService.changeUserRole(userId, request);
        return ResponseEntity.noContent().build();
    }
}