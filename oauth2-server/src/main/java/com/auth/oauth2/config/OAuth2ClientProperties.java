package com.auth.oauth2.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "oauth2.clients")
@Getter
@Setter
public class OAuth2ClientProperties {

  private DefaultClients defaultClients = new DefaultClients();
  private TestClient testClient = new TestClient();
  private TokenSettings defaultTokenSettings = new TokenSettings();

  @Getter
  @Setter
  public static class TokenSettings {
    private Duration accessTokenTtl = Duration.ofHours(24); // 24 hours
    private Duration refreshTokenTtl = Duration.ofDays(7); // 7 days
  }

  @Getter
  @Setter
  public static class DefaultClients {
    private Slack slack = new Slack();

    @Getter
    @Setter
    public static class Slack {
      private boolean enabled = true;
      private String clientId = "slack";
      private String clientSecret = "slack-secret-key";
      private List<String> redirectUris =
          new ArrayList<>(
              List.of(
                  "http://localhost:3000/auth/callback",
                  "http://localhost:3000/signup/callback",
                  "http://localhost:3000/callback"));
      private List<String> scopes = new ArrayList<>(List.of("read", "write"));
      private List<String> grantTypes =
          new ArrayList<>(
              List.of("authorization_code", "refresh_token", "client_credentials"));
    }
  }

  @Getter
  @Setter
  public static class TestClient {
    private boolean enabled = false; // Disabled by default in production
    private String clientId = "test-client";
    private String clientSecret = "test-secret-key";
    private List<String> redirectUris = new ArrayList<>();
    private List<String> scopes =
        new ArrayList<>(List.of("read", "write", "openid", "profile", "email"));
    private List<String> grantTypes =
        new ArrayList<>(
            List.of("authorization_code", "refresh_token", "client_credentials"));
    private TokenSettings tokenSettings = new TokenSettings();

    @Getter
    @Setter
    public static class TokenSettings {
      // 100 years = 36500 days (effectively never expires)
      private Duration accessTokenTtl = Duration.ofDays(36500);
      private Duration refreshTokenTtl = Duration.ofDays(36500);
    }
  }
}

