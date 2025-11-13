package com.auth.server.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

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
    given(jwtConfig.getSecret()).willReturn(TEST_SECRET);
    given(jwtConfig.getExpiration()).willReturn(TEST_EXPIRATION);
    given(jwtConfig.getRefreshExpiration()).willReturn(TEST_REFRESH_EXPIRATION);
    jwtTokenProvider = new JwtTokenProvider(jwtConfig);
  }

  @Test
  @DisplayName("generateAccessToken: 토큰 생성 성공")
  void shouldGenerateTokenSuccessfully() {
    // given
    var username = "testuser";
    var userId = 1L;

    // when
    var token = jwtTokenProvider.generateAccessToken(username, userId);

    // then
    assertThat(token).isNotBlank();
    assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
  }

  @Test
  @DisplayName("getUsernameFromToken: 토큰에서 사용자명 추출")
  void shouldExtractUsernameFromToken() {
    // given
    var username = "testuser";
    var userId = 1L;
    var token = jwtTokenProvider.generateAccessToken(username, userId);

    // when
    var extractedUsername = jwtTokenProvider.getUsernameFromToken(token);

    // then
    assertThat(extractedUsername).isEqualTo(username);
  }

  @Test
  @DisplayName("validateToken: 유효한 토큰 검증")
  void shouldValidateValidToken() {
    // given
    var username = "testuser";
    var userId = 1L;
    var token = jwtTokenProvider.generateAccessToken(username, userId);

    // when
    var isValid = jwtTokenProvider.validateToken(token);

    // then
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("인증 실패: 유효하지 않은 토큰")
  void shouldFailValidationForInvalidToken() {
    // given
    var invalidToken = "invalid.token.here";

    // when
    var isValid = jwtTokenProvider.validateToken(invalidToken);

    // then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("인증 실패: 빈 토큰")
  void shouldFailValidationForEmptyToken() {
    // given
    var emptyToken = "";

    // when
    var isValid = jwtTokenProvider.validateToken(emptyToken);

    // then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("인증 실패: null 토큰")
  void shouldFailValidationForNullToken() {
    // given
    String nullToken = null;

    // when
    var isValid = jwtTokenProvider.validateToken(nullToken);

    // then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("인증 실패: 다른 시크릿으로 생성된 토큰")
  void shouldFailValidationForTokenWithDifferentSecret() {
    // given
    var username = "testuser";
    var userId = 1L;
    var differentSecret =
        "differentSecretKeyForTesting12345678901234567890123456789012345678901234567890";

    // Create token with different secret using reflection or manual creation
    var differentConfig = mock(JwtConfig.class);
    given(differentConfig.getSecret()).willReturn(differentSecret);
    given(differentConfig.getExpiration()).willReturn(TEST_EXPIRATION);
    given(differentConfig.getRefreshExpiration()).willReturn(TEST_REFRESH_EXPIRATION);

    var differentProvider = new JwtTokenProvider(differentConfig);
    var tokenWithDifferentSecret = differentProvider.generateAccessToken(username, userId);

    // when
    var isValid = jwtTokenProvider.validateToken(tokenWithDifferentSecret);

    // then
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("isRefreshToken: Refresh Token 식별")
  void shouldIdentifyRefreshToken() {
    // given
    var username = "testuser";
    var userId = 1L;
    var refreshToken = jwtTokenProvider.generateRefreshToken(username, userId);

    // when
    var isRefresh = jwtTokenProvider.isRefreshToken(refreshToken);

    // then
    assertThat(isRefresh).isTrue();
  }
}
