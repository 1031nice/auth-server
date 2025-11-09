package com.auth.server.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.auth.server.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JWT Token Provider tests")
class JwtTokenProviderTest {

  @Mock private JwtConfig jwtConfig;

  private JwtTokenProvider jwtTokenProvider;

  private static final String TEST_SECRET =
      "mySecretKeyForTesting123456789012345678901234567890123456789012345678901234";
  private static final Long TEST_EXPIRATION = 3600000L; // 1 hour
  private static final Long TEST_REFRESH_EXPIRATION = 604800000L; // 7 days

  @BeforeEach
  void setUp() {
    when(jwtConfig.getSecret()).thenReturn(TEST_SECRET);
    when(jwtConfig.getExpiration()).thenReturn(TEST_EXPIRATION);
    when(jwtConfig.getRefreshExpiration()).thenReturn(TEST_REFRESH_EXPIRATION);
    jwtTokenProvider = new JwtTokenProvider(jwtConfig);
  }

  @Test
  @DisplayName("Should generate token successfully")
  void shouldGenerateTokenSuccessfully() {
    // given
    String username = "testuser";
    Long userId = 1L;

    // when
    String token = jwtTokenProvider.generateAccessToken(username, userId);

    // then
    assertThat(token).isNotBlank();
    assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
  }

  @Test
  @DisplayName("Should extract username from token")
  void shouldExtractUsernameFromToken() {
    // given
    String username = "testuser";
    Long userId = 1L;
    String token = jwtTokenProvider.generateAccessToken(username, userId);

    // when
    String extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

    // then
    assertThat(extractedUsername).isEqualTo(username);
  }

  @Test
  @DisplayName("Should validate valid token")
  void shouldValidateValidToken() {
    // given
    String username = "testuser";
    Long userId = 1L;
    String token = jwtTokenProvider.generateAccessToken(username, userId);

    // when
    boolean isValid = jwtTokenProvider.validateToken(token);

    // then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("Should fail validation for invalid token")
  void shouldFailValidationForInvalidToken() {
    // given
    String invalidToken = "invalid.token.here";

    // when
    boolean isValid = jwtTokenProvider.validateToken(invalidToken);

    // then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should fail validation for empty token")
  void shouldFailValidationForEmptyToken() {
    // given
    String emptyToken = "";

    // when
    boolean isValid = jwtTokenProvider.validateToken(emptyToken);

    // then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should fail validation for null token")
  void shouldFailValidationForNullToken() {
    // given
    String nullToken = null;

    // when
    boolean isValid = jwtTokenProvider.validateToken(nullToken);

    // then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should fail validation for token with different secret")
  void shouldFailValidationForTokenWithDifferentSecret() {
    // given
    String username = "testuser";
    Long userId = 1L;
    String differentSecret =
        "differentSecretKeyForTesting12345678901234567890123456789012345678901234567890";

    // Create token with different secret using reflection or manual creation
    JwtConfig differentConfig = org.mockito.Mockito.mock(JwtConfig.class);
    org.mockito.Mockito.when(differentConfig.getSecret()).thenReturn(differentSecret);
    org.mockito.Mockito.when(differentConfig.getExpiration()).thenReturn(TEST_EXPIRATION);
    org.mockito.Mockito.when(differentConfig.getRefreshExpiration())
        .thenReturn(TEST_REFRESH_EXPIRATION);

    JwtTokenProvider differentProvider = new JwtTokenProvider(differentConfig);
    String tokenWithDifferentSecret = differentProvider.generateAccessToken(username, userId);

    // when
    boolean isValid = jwtTokenProvider.validateToken(tokenWithDifferentSecret);

    // then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("Should identify refresh token correctly")
  void shouldIdentifyRefreshToken() {
    // given
    String username = "testuser";
    Long userId = 1L;
    String refreshToken = jwtTokenProvider.generateRefreshToken(username, userId);

    // when
    boolean isRefresh = jwtTokenProvider.isRefreshToken(refreshToken);

    // then
    assertThat(isRefresh).isTrue();
  }
}
