package com.auth.server.controller;

import com.auth.server.domain.dto.request.LoginRequest;
import com.auth.server.domain.dto.request.TokenRefreshRequest;
import com.auth.server.domain.dto.response.AuthResponse;
import com.auth.server.security.rate.annotation.RateLimit;
import com.auth.server.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  @RateLimit("login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    AuthResponse response = authService.login(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh")
  @RateLimit("refresh")
  public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
    AuthResponse response = authService.refreshToken(request.getRefreshToken());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/health")
  @RateLimit
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("Auth Server is running");
  }
}
