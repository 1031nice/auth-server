package com.auth.oauth2.service;

import com.auth.oauth2.domain.dto.request.SignupRequest;
import com.auth.oauth2.domain.entity.OAuth2Client;
import com.auth.oauth2.domain.entity.Role;
import com.auth.oauth2.domain.entity.User;
import com.auth.oauth2.repository.OAuth2ClientRepository;
import com.auth.oauth2.repository.UserRepository;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final OAuth2ClientRepository clientRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public User signup(SignupRequest request) {
    // Username 중복 검증
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new RuntimeException("Username already exists");
    }

    // Email 중복 검증
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new RuntimeException("Email already exists");
    }

    // redirect_uri 검증 (clientId가 제공된 경우)
    if (request.getClientId() != null && request.getRedirectUri() != null) {
      validateRedirectUri(request.getRedirectUri(), request.getClientId());
    }

    // 비밀번호 암호화
    String encodedPassword = passwordEncoder.encode(request.getPassword());

    // User 엔티티 생성
    User user =
        User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(encodedPassword)
            .roles(Collections.singletonList(Role.ROLE_USER))
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .build();

    return userRepository.save(user);
  }

  private void validateRedirectUri(String redirectUri, String clientId) {
    OAuth2Client client =
        clientRepository
            .findByClientId(clientId)
            .orElseThrow(() -> new RuntimeException("Invalid client ID"));

    if (!client.getRedirectUris().contains(redirectUri)) {
      throw new RuntimeException("Redirect URI is not registered for this client");
    }
  }
}

