package com.auth.oauth2.security;

import com.auth.oauth2.config.OAuth2ClientProperties;
import com.auth.oauth2.domain.entity.OAuth2Client;
import com.auth.oauth2.repository.OAuth2ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2RegisteredClientRepository implements RegisteredClientRepository {

  private final OAuth2ClientRepository clientRepository;
  private final OAuth2ClientProperties clientProperties;

  @Override
  public void save(RegisteredClient registeredClient) {
    // This is handled by our OAuth2ClientService
    throw new UnsupportedOperationException("Use OAuth2ClientService to create clients instead");
  }

  @Override
  public RegisteredClient findById(String id) {
    return clientRepository.findById(Long.parseLong(id)).map(this::toRegisteredClient).orElse(null);
  }

  @Override
  public RegisteredClient findByClientId(String clientId) {
    return clientRepository.findByClientId(clientId).map(this::toRegisteredClient).orElse(null);
  }

  private RegisteredClient toRegisteredClient(OAuth2Client client) {
    // OAuth2 client secrets are compared directly by Spring Security OAuth2
    // We store them as plain text (or you can encrypt at DB level)
    RegisteredClient.Builder builder =
        RegisteredClient.withId(String.valueOf(client.getId()))
            .clientId(client.getClientId())
            .clientSecret(client.getClientSecret())
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);

    // Add grant types
    if (client.getGrantTypes() != null) {
      client
          .getGrantTypes()
          .forEach(gt -> builder.authorizationGrantType(new AuthorizationGrantType(gt)));
    }

    // Add redirect URIs
    if (client.getRedirectUris() != null) {
      client.getRedirectUris().forEach(builder::redirectUri);
    }

    // Add scopes
    if (client.getScopes() != null) {
      client.getScopes().forEach(builder::scope);
    }

    // Default scopes if none specified
    if (client.getScopes() == null || client.getScopes().isEmpty()) {
      builder.scope("read").scope("write");
    }

    var tokenSettings = getTokenSettings(client);

    return builder
        .clientSettings(
            ClientSettings.builder()
                .requireAuthorizationConsent(false)
                // PKCE is optional: clients can use PKCE if they want, but it's not required
                // This allows registered confidential clients to work without PKCE
                .requireProofKey(false)
                .build())
        .tokenSettings(tokenSettings)
        .build();
  }

  private TokenSettings getTokenSettings(OAuth2Client client) {
    // Use custom TTL if client has one, otherwise use default settings
    Duration accessTokenTtl =
        client.getCustomAccessTokenTtlSeconds() != null
            ? Duration.ofSeconds(client.getCustomAccessTokenTtlSeconds())
            : clientProperties.getDefaultTokenSettings().getAccessTokenTtl();

    Duration refreshTokenTtl =
        client.getCustomRefreshTokenTtlSeconds() != null
            ? Duration.ofSeconds(client.getCustomRefreshTokenTtlSeconds())
            : clientProperties.getDefaultTokenSettings().getRefreshTokenTtl();

    return TokenSettings.builder()
        .accessTokenTimeToLive(accessTokenTtl)
        .refreshTokenTimeToLive(refreshTokenTtl)
        .reuseRefreshTokens(false) // Refresh Token Rotation (RTR) enabled
        .build();
  }
}
