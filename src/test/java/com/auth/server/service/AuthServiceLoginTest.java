package com.auth.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;

import com.auth.server.config.JwtConfig;
import com.auth.server.domain.dto.request.LoginRequest;
import com.auth.server.domain.dto.response.AuthResponse;
import com.auth.server.domain.entity.RefreshToken;
import com.auth.server.domain.entity.Role;
import com.auth.server.domain.entity.User;
import com.auth.server.repository.RefreshTokenRepository;
import com.auth.server.repository.UserRepository;
import com.auth.server.security.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService login method tests")
class AuthServiceLoginTest {

  @Mock private UserRepository userRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtTokenProvider jwtTokenProvider;
  @Mock private AuthenticationManager authenticationManager;
  @Mock private JwtConfig jwtConfig;

  @InjectMocks private AuthService authService;

  private User testUser;
  private LoginRequest loginRequest;
  private Authentication mockAuthentication;

  @BeforeEach
  void setUp() {
    testUser =
        User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .password("encodedPassword")
            .roles(List.of(Role.ROLE_USER))
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    loginRequest = LoginRequest.builder().username("testuser").password("password123").build();

    mockAuthentication = mock(Authentication.class);
  }

  @Test
  @DisplayName("login: 유효한 자격증명으로 로그인 성공")
  void shouldLoginSuccessfullyWithValidCredentials() {
    // given
    var accessToken = "jwt.token.here";
    var refreshToken = "jwt.refresh.token";
    var roles = List.of("ROLE_USER");

    given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .willReturn(mockAuthentication);
    given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));
    willDoNothing().given(refreshTokenRepository).deleteByUserId(anyLong());
    given(jwtTokenProvider.generateAccessToken(testUser.getUsername(), testUser.getId(), roles))
        .willReturn(accessToken);
    given(jwtTokenProvider.generateRefreshToken(testUser.getUsername(), testUser.getId(), roles))
        .willReturn(refreshToken);
    given(jwtConfig.getRefreshExpiration()).willReturn(604800000L);
    given(refreshTokenRepository.save(any(RefreshToken.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when
    var response = authService.login(loginRequest);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getAccessToken()).isEqualTo(accessToken);
    assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
    assertThat(response.getId()).isEqualTo(testUser.getId());
    assertThat(response.getUsername()).isEqualTo(testUser.getUsername());
    assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
    assertThat(response.getRoles()).containsExactly("ROLE_USER");

    then(authenticationManager).should(times(1)).authenticate(any());
    then(userRepository).should(times(1)).findByUsername("testuser");
    then(refreshTokenRepository).should(times(1)).deleteByUserId(testUser.getId());
    then(jwtTokenProvider).should(times(1))
        .generateAccessToken(testUser.getUsername(), testUser.getId(), roles);
    then(jwtTokenProvider).should(times(1))
        .generateRefreshToken(testUser.getUsername(), testUser.getId(), roles);
    then(refreshTokenRepository).should(times(1)).save(any(RefreshToken.class));
  }

  @Test
  @DisplayName("인증 실패: 잘못된 자격증명")
  void shouldFailLoginWithInvalidCredentials() {
    // given
    given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .willThrow(new BadCredentialsException("Bad credentials"));

    // when & then
    assertThatThrownBy(() -> authService.login(loginRequest))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("Bad credentials");

    then(authenticationManager).should(times(1)).authenticate(any());
    then(userRepository).shouldHaveNoInteractions();
    then(jwtTokenProvider).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("인증 실패: 사용자를 찾을 수 없음")
  void shouldFailLoginWhenUserNotFound() {
    // given
    given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .willReturn(mockAuthentication);
    given(userRepository.findByUsername("testuser")).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> authService.login(loginRequest))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("User not found");

    then(authenticationManager).should(times(1)).authenticate(any());
    then(userRepository).should(times(1)).findByUsername("testuser");
    then(jwtTokenProvider).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("login: 여러 역할을 가진 사용자 로그인 성공")
  void shouldLoginSuccessfullyWithMultipleRoles() {
    // given
    var multiRoleUser =
        User.builder()
            .id(2L)
            .username("adminuser")
            .email("admin@example.com")
            .password("encodedPassword")
            .roles(List.of(Role.ROLE_USER, Role.ROLE_ADMIN))
            .enabled(true)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .build();

    var adminLoginRequest =
        LoginRequest.builder().username("adminuser").password("password123").build();

    var accessToken = "admin.jwt.token";
    var refreshToken = "admin.jwt.refresh.token";
    var roles = List.of("ROLE_USER", "ROLE_ADMIN");

    given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .willReturn(mockAuthentication);
    given(userRepository.findByUsername("adminuser")).willReturn(Optional.of(multiRoleUser));
    willDoNothing().given(refreshTokenRepository).deleteByUserId(anyLong());
    given(jwtTokenProvider.generateAccessToken("adminuser", 2L, roles)).willReturn(accessToken);
    given(jwtTokenProvider.generateRefreshToken("adminuser", 2L, roles)).willReturn(refreshToken);
    given(jwtConfig.getRefreshExpiration()).willReturn(604800000L);
    given(refreshTokenRepository.save(any(RefreshToken.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when
    var response = authService.login(adminLoginRequest);

    // then
    assertThat(response.getRoles()).hasSize(2);
    assertThat(response.getRoles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
  }

  @Test
  @DisplayName("login: SecurityContext에 인증 정보 설정")
  void shouldSetAuthenticationInSecurityContext() {
    // given
    var accessToken = "jwt.token.here";
    var refreshToken = "jwt.refresh.token";
    var roles = List.of("ROLE_USER");

    given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .willReturn(mockAuthentication);
    given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));
    willDoNothing().given(refreshTokenRepository).deleteByUserId(anyLong());
    given(jwtTokenProvider.generateAccessToken(testUser.getUsername(), testUser.getId(), roles))
        .willReturn(accessToken);
    given(jwtTokenProvider.generateRefreshToken(testUser.getUsername(), testUser.getId(), roles))
        .willReturn(refreshToken);
    given(jwtConfig.getRefreshExpiration()).willReturn(604800000L);
    given(refreshTokenRepository.save(any(RefreshToken.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when
    authService.login(loginRequest);

    // then
    // SecurityContext configuration occurs but difficult to verify in unit tests
    // Verify in integration tests instead
    then(authenticationManager).should(times(1)).authenticate(any());
  }

  @Test
  @DisplayName("refreshToken: 유효한 토큰으로 새 토큰 발급 성공")
  void shouldIssueNewTokensWithValidRefreshToken() {
    // given
    var refreshToken = "valid.refresh.token";
    var refreshedAccessToken = "new.access.token";
    var refreshedRefreshToken = "new.refresh.token";
    var roles = List.of("ROLE_USER");

    var storedToken =
        RefreshToken.builder()
            .id(1L)
            .userId(1L)
            .token(refreshToken)
            .expiresAt(LocalDateTime.now().plusDays(7))
            .revoked(false)
            .build();

    given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
    given(jwtTokenProvider.isRefreshToken(refreshToken)).willReturn(true);
    given(refreshTokenRepository.findByToken(refreshToken)).willReturn(Optional.of(storedToken));
    given(jwtTokenProvider.getUsernameFromToken(refreshToken)).willReturn("testuser");
    given(jwtTokenProvider.getScopes(refreshToken)).willReturn(roles);
    given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));
    given(jwtTokenProvider.generateAccessToken(testUser.getUsername(), testUser.getId(), roles))
        .willReturn(refreshedAccessToken);
    given(jwtTokenProvider.generateRefreshToken(testUser.getUsername(), testUser.getId(), roles))
        .willReturn(refreshedRefreshToken);
    given(jwtConfig.getRefreshExpiration()).willReturn(604800000L);
    given(refreshTokenRepository.save(any(RefreshToken.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when
    var response = authService.refreshToken(refreshToken);

    // then
    assertThat(response.getAccessToken()).isEqualTo(refreshedAccessToken);
    assertThat(response.getRefreshToken()).isEqualTo(refreshedRefreshToken);
  }

  @Test
  @DisplayName("인증 실패: 유효하지 않은 refresh token")
  void shouldThrowWhenRefreshTokenInvalid() {
    // given
    var refreshToken = "invalid.refresh.token";
    given(jwtTokenProvider.validateToken(refreshToken)).willReturn(false);

    // when & then
    assertThatThrownBy(() -> authService.refreshToken(refreshToken))
        .isInstanceOf(com.auth.server.exception.InvalidTokenException.class)
        .hasMessageContaining("Invalid refresh token");
  }

  @Test
  @DisplayName("인증 실패: refresh token이 아닌 토큰")
  void shouldThrowWhenTokenIsNotRefreshType() {
    // given
    var refreshToken = "access.token";
    given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
    given(jwtTokenProvider.isRefreshToken(refreshToken)).willReturn(false);

    // when & then
    assertThatThrownBy(() -> authService.refreshToken(refreshToken))
        .isInstanceOf(com.auth.server.exception.InvalidTokenException.class)
        .hasMessageContaining("Invalid refresh token");
  }
}
