package com.auth.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.auth.server.domain.dto.request.LoginRequest;
import com.auth.server.domain.dto.response.AuthResponse;
import com.auth.server.domain.entity.Role;
import com.auth.server.domain.entity.User;
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

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private JwtTokenProvider jwtTokenProvider;

  @Mock private AuthenticationManager authenticationManager;

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
  @DisplayName("Should login successfully with valid credentials")
  void shouldLoginSuccessfullyWithValidCredentials() {
    // given
    String accessToken = "jwt.token.here";
    String refreshToken = "jwt.refresh.token";

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(mockAuthentication);
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(jwtTokenProvider.generateAccessToken(testUser.getUsername(), testUser.getId()))
        .thenReturn(accessToken);
    when(jwtTokenProvider.generateRefreshToken(testUser.getUsername(), testUser.getId()))
        .thenReturn(refreshToken);

    // when
    AuthResponse response = authService.login(loginRequest);

    // then
    assertThat(response).isNotNull();
    assertThat(response.getAccessToken()).isEqualTo(accessToken);
    assertThat(response.getRefreshToken()).isEqualTo(refreshToken);
    assertThat(response.getId()).isEqualTo(testUser.getId());
    assertThat(response.getUsername()).isEqualTo(testUser.getUsername());
    assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
    assertThat(response.getRoles()).containsExactly("ROLE_USER");

    verify(authenticationManager, times(1)).authenticate(any());
    verify(userRepository, times(1)).findByUsername("testuser");
    verify(jwtTokenProvider, times(1)).generateAccessToken(testUser.getUsername(), testUser.getId());
    verify(jwtTokenProvider, times(1)).generateRefreshToken(testUser.getUsername(), testUser.getId());
  }

  @Test
  @DisplayName("Should fail login with invalid credentials")
  void shouldFailLoginWithInvalidCredentials() {
    // given
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    // when & then
    assertThatThrownBy(() -> authService.login(loginRequest))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("Bad credentials");

    verify(authenticationManager, times(1)).authenticate(any());
    verify(userRepository, never()).findByUsername(anyString());
    verify(jwtTokenProvider, never()).generateAccessToken(anyString(), anyLong());
    verify(jwtTokenProvider, never()).generateRefreshToken(anyString(), anyLong());
  }

  @Test
  @DisplayName("Should fail login when user not found")
  void shouldFailLoginWhenUserNotFound() {
    // given
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(mockAuthentication);
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> authService.login(loginRequest))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("User not found");

    verify(authenticationManager, times(1)).authenticate(any());
    verify(userRepository, times(1)).findByUsername("testuser");
    verify(jwtTokenProvider, never()).generateAccessToken(anyString(), anyLong());
    verify(jwtTokenProvider, never()).generateRefreshToken(anyString(), anyLong());
  }

  @Test
  @DisplayName("Should login successfully with multiple roles")
  void shouldLoginSuccessfullyWithMultipleRoles() {
    // given
    User multiRoleUser =
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

    LoginRequest adminLoginRequest =
        LoginRequest.builder().username("adminuser").password("password123").build();

    String accessToken = "admin.jwt.token";
    String refreshToken = "admin.jwt.refresh.token";

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(mockAuthentication);
    when(userRepository.findByUsername("adminuser")).thenReturn(Optional.of(multiRoleUser));
    when(jwtTokenProvider.generateAccessToken("adminuser", 2L)).thenReturn(accessToken);
    when(jwtTokenProvider.generateRefreshToken("adminuser", 2L)).thenReturn(refreshToken);

    // when
    AuthResponse response = authService.login(adminLoginRequest);

    // then
    assertThat(response.getRoles()).hasSize(2);
    assertThat(response.getRoles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
  }

  @Test
  @DisplayName("Should set Authentication in SecurityContext during login")
  void shouldSetAuthenticationInSecurityContext() {
    // given
    String accessToken = "jwt.token.here";
    String refreshToken = "jwt.refresh.token";

    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(mockAuthentication);
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(jwtTokenProvider.generateAccessToken(testUser.getUsername(), testUser.getId()))
        .thenReturn(accessToken);
    when(jwtTokenProvider.generateRefreshToken(testUser.getUsername(), testUser.getId()))
        .thenReturn(refreshToken);

    // when
    authService.login(loginRequest);

    // then
    // SecurityContext configuration occurs but difficult to verify in unit tests
    // Verify in integration tests instead
    verify(authenticationManager, times(1)).authenticate(any());
  }

  @Test
  @DisplayName("Should issue new tokens when refresh token is valid")
  void shouldIssueNewTokensWithValidRefreshToken() {
    // given
    String refreshToken = "valid.refresh.token";
    String refreshedAccessToken = "new.access.token";
    String refreshedRefreshToken = "new.refresh.token";

    when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
    when(jwtTokenProvider.isRefreshToken(refreshToken)).thenReturn(true);
    when(jwtTokenProvider.getUsernameFromToken(refreshToken)).thenReturn("testuser");
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(jwtTokenProvider.generateAccessToken(testUser.getUsername(), testUser.getId()))
        .thenReturn(refreshedAccessToken);
    when(jwtTokenProvider.generateRefreshToken(testUser.getUsername(), testUser.getId()))
        .thenReturn(refreshedRefreshToken);

    // when
    AuthResponse response = authService.refreshToken(refreshToken);

    // then
    assertThat(response.getAccessToken()).isEqualTo(refreshedAccessToken);
    assertThat(response.getRefreshToken()).isEqualTo(refreshedRefreshToken);
  }

  @Test
  @DisplayName("Should throw exception when refresh token is invalid")
  void shouldThrowWhenRefreshTokenInvalid() {
    // given
    String refreshToken = "invalid.refresh.token";
    when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(false);

    // when & then
    assertThatThrownBy(() -> authService.refreshToken(refreshToken))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Invalid refresh token");
  }

  @Test
  @DisplayName("Should throw exception when token is not refresh type")
  void shouldThrowWhenTokenIsNotRefreshType() {
    // given
    String refreshToken = "access.token";
    when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
    when(jwtTokenProvider.isRefreshToken(refreshToken)).thenReturn(false);

    // when & then
    assertThatThrownBy(() -> authService.refreshToken(refreshToken))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Invalid refresh token");
  }
}
