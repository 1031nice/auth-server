package com.auth.oauth2.config;

import com.auth.oauth2.domain.entity.OAuth2Client;
import com.auth.oauth2.domain.entity.Role;
import com.auth.oauth2.domain.entity.User;
import com.auth.oauth2.repository.OAuth2ClientRepository;
import com.auth.oauth2.repository.UserRepository;
import java.util.ArrayList;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer {

  private final UserRepository userRepository;
  private final OAuth2ClientRepository clientRepository;
  private final OAuth2ClientProperties clientProperties;
  // Create BCryptPasswordEncoder directly instead of registering as bean to avoid bean conflicts
  private final BCryptPasswordEncoder userPasswordEncoder = new BCryptPasswordEncoder();

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void initializeDefaultData() {
    initializeDefaultUsers();
    initializeDefaultClients();
  }

  private void initializeDefaultUsers() {
    // Create default test user
    if (!userRepository.existsByEmail("t@t.com")) {
      User testUser =
          User.builder()
              .email("t@t.com")
              .password(userPasswordEncoder.encode("1234"))
              .roles(Collections.singletonList(Role.ROLE_USER))
              .enabled(true)
              .accountNonExpired(true)
              .accountNonLocked(true)
              .credentialsNonExpired(true)
              .build();

      userRepository.save(testUser);
      log.info("Default test user created: t@t.com / 1234");
    } else {
      log.debug("Default test user already exists");
    }
  }

  private void initializeDefaultClients() {
    // Slack client
    if (clientProperties.getDefaultClients().getSlack().isEnabled()) {
      initializeSlackClient();
    }

    // Test client (conditional)
    if (clientProperties.getTestClient().isEnabled()) {
      initializeTestClient();
    }
  }

  private void initializeSlackClient() {
    var slackConfig = clientProperties.getDefaultClients().getSlack();

    if (!clientRepository.existsByClientId(slackConfig.getClientId())) {
      OAuth2Client slackClient =
          OAuth2Client.builder()
              .clientId(slackConfig.getClientId())
              .clientSecret(slackConfig.getClientSecret())
              .redirectUris(new ArrayList<>(slackConfig.getRedirectUris()))
              .scopes(new ArrayList<>(slackConfig.getScopes()))
              .grantTypes(new ArrayList<>(slackConfig.getGrantTypes()))
              .enabled(true)
              .build();

      clientRepository.save(slackClient);
      log.info(
          "Slack client created: clientId={}, redirectUris={}",
          slackConfig.getClientId(),
          slackConfig.getRedirectUris());
    } else {
      log.debug("Slack client already exists");
    }
  }

  private void initializeTestClient() {
    var testConfig = clientProperties.getTestClient();

    if (!clientRepository.existsByClientId(testConfig.getClientId())) {
      // Custom TTL for test client (100 years = effectively never expires)
      long customTtlSeconds = testConfig.getTokenSettings().getAccessTokenTtl().getSeconds();

      OAuth2Client testClient =
          OAuth2Client.builder()
              .clientId(testConfig.getClientId())
              .clientSecret(testConfig.getClientSecret())
              .redirectUris(new ArrayList<>(testConfig.getRedirectUris()))
              .scopes(new ArrayList<>(testConfig.getScopes()))
              .grantTypes(new ArrayList<>(testConfig.getGrantTypes()))
              .enabled(true)
              .customAccessTokenTtlSeconds(customTtlSeconds)
              .customRefreshTokenTtlSeconds(customTtlSeconds)
              .build();

      clientRepository.save(testClient);
      log.info(
          "Test client created with custom TTL: clientId={}, accessTokenTtl={}days, "
              + "refreshTokenTtl={}days, redirectUris={}",
          testConfig.getClientId(),
          customTtlSeconds / 86400,
          customTtlSeconds / 86400,
          testConfig.getRedirectUris());
    } else {
      log.debug("Test client already exists");
    }
  }
}

