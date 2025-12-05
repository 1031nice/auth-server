package com.auth.oauth2.domain.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuth2ClientRequest {

  @NotBlank(message = "Client ID is required")
  private String clientId;

  @NotBlank(message = "Client secret is required")
  private String clientSecret;

  private List<String> redirectUris;

  private List<String> scopes;

  @NotEmpty(message = "At least one grant type is required")
  private List<String> grantTypes;

  /**
   * Custom access token TTL in seconds.
   * Uses default settings if null.
   */
  @Min(value = 1, message = "Access token TTL must be at least 1 second")
  private Long customAccessTokenTtlSeconds;

  /**
   * Custom refresh token TTL in seconds.
   * Uses default settings if null.
   */
  @Min(value = 1, message = "Refresh token TTL must be at least 1 second")
  private Long customRefreshTokenTtlSeconds;
}
