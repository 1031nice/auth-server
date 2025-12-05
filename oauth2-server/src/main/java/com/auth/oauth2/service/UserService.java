package com.auth.oauth2.service;

import com.auth.oauth2.domain.dto.request.SignupRequest;
import com.auth.oauth2.domain.entity.OAuth2Client;
import com.auth.oauth2.domain.entity.Role;
import com.auth.oauth2.domain.entity.User;
import com.auth.oauth2.repository.OAuth2ClientRepository;
import com.auth.oauth2.repository.UserRepository;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final OAuth2ClientRepository clientRepository;
  // Create BCryptPasswordEncoder directly instead of registering as bean to avoid bean conflicts
  private final BCryptPasswordEncoder userPasswordEncoder = new BCryptPasswordEncoder();

  @Transactional
  public User signup(SignupRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new RuntimeException("Email already exists");
    }

    // Validate redirect_uri (if clientId is provided)
    if (request.getClientId() != null
        && !request.getClientId().isBlank()
        && request.getRedirectUri() != null
        && !request.getRedirectUri().isBlank()) {
      validateRedirectUri(request.getRedirectUri(), request.getClientId());
    }

    String encodedPassword = userPasswordEncoder.encode(request.getPassword());

    User user =
        User.builder()
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

