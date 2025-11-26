package com.auth.oauth2.config;

import com.auth.oauth2.domain.entity.OAuth2Client;
import com.auth.oauth2.domain.entity.Role;
import com.auth.oauth2.domain.entity.User;
import com.auth.oauth2.repository.OAuth2ClientRepository;
import com.auth.oauth2.repository.UserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

  private final UserRepository userRepository;
  private final OAuth2ClientRepository clientRepository;
  private final PasswordEncoder passwordEncoder;

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void initializeDefaultData() {
    initializeDefaultUsers();
    initializeDefaultClients();
  }

  private void initializeDefaultUsers() {
    // Create default test user
    if (!userRepository.existsByEmail("test@example.com")) {
      User testUser =
          User.builder()
              .email("test@example.com")
              .password(passwordEncoder.encode("password123"))
              .roles(Collections.singletonList(Role.ROLE_USER))
              .enabled(true)
              .accountNonExpired(true)
              .accountNonLocked(true)
              .credentialsNonExpired(true)
              .build();

      userRepository.save(testUser);
      log.info("Default test user created: test@example.com / password123");
    } else {
      log.debug("Default test user already exists");
    }
  }

  private void initializeDefaultClients() {
    // Create default slack client
    if (!clientRepository.existsByClientId("slack")) {
      List<String> redirectUris =
          Arrays.asList(
              "http://localhost:3000/auth/callback",
              "http://localhost:3000/signup/callback",
              "http://localhost:3000/callback");
      List<String> scopes = Arrays.asList("read", "write");
      List<String> grantTypes =
          Arrays.asList("authorization_code", "refresh_token", "client_credentials");

      OAuth2Client slackClient =
          OAuth2Client.builder()
              .clientId("slack")
              .clientSecret("slack-secret-key")
              .redirectUris(redirectUris)
              .scopes(scopes)
              .grantTypes(grantTypes)
              .enabled(true)
              .build();

      clientRepository.save(slackClient);
      log.info(
          "Default slack client created: clientId=slack, clientSecret=slack-secret-key, redirectUris={}",
          redirectUris);
    } else {
      log.debug("Default slack client already exists");
    }
  }
}

