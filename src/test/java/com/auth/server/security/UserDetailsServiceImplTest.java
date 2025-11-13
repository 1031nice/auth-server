package com.auth.server.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

import com.auth.server.domain.entity.Role;
import com.auth.server.domain.entity.User;
import com.auth.server.repository.UserRepository;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsService tests")
class UserDetailsServiceImplTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private UserDetailsServiceImpl userDetailsService;

  private User testUser;

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
  }

  @Test
  @DisplayName("loadUserByUsername: 존재하는 사용자로 UserDetails 로드")
  void shouldLoadUserDetailsByUsername() {
    // given
    var username = "testuser";
    given(userRepository.findByUsername(username)).willReturn(Optional.of(testUser));

    // when
    var userDetails = userDetailsService.loadUserByUsername(username);

    // then
    assertThat(userDetails).isNotNull();
    assertThat(userDetails.getUsername()).isEqualTo(username);
    assertThat(userDetails.getPassword()).isEqualTo(testUser.getPassword());
    assertThat(userDetails.getAuthorities()).hasSize(1);
    assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
        .isEqualTo("ROLE_USER");
    assertThat(userDetails.isEnabled()).isTrue();
    assertThat(userDetails.isAccountNonExpired()).isTrue();
    assertThat(userDetails.isAccountNonLocked()).isTrue();
    assertThat(userDetails.isCredentialsNonExpired()).isTrue();
  }

  @Test
  @DisplayName("인증 실패: 사용자를 찾을 수 없음")
  void shouldThrowExceptionWhenUserNotFound() {
    // given
    var username = "nonexistent";
    given(userRepository.findByUsername(username)).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> userDetailsService.loadUserByUsername(username))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessageContaining("User not found: " + username);
  }

  @Test
  @DisplayName("loadUserByUsername: 여러 역할을 가진 사용자 로드")
  void shouldLoadUserDetailsWithMultipleRoles() {
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

    given(userRepository.findByUsername("adminuser")).willReturn(Optional.of(multiRoleUser));

    // when
    var userDetails = userDetailsService.loadUserByUsername("adminuser");

    // then
    assertThat(userDetails.getAuthorities()).hasSize(2);
    assertThat(userDetails.getAuthorities().stream().map(auth -> auth.getAuthority()).toList())
        .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
  }

  @Test
  @DisplayName("loadUserByUsername: 비활성화된 사용자 로드")
  void shouldLoadUserDetailsForDisabledUser() {
    // given
    var disabledUser =
        User.builder()
            .id(3L)
            .username("disableduser")
            .email("disabled@example.com")
            .password("encodedPassword")
            .roles(List.of(Role.ROLE_USER))
            .enabled(false)
            .accountNonExpired(true)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .build();

    given(userRepository.findByUsername("disableduser")).willReturn(Optional.of(disabledUser));

    // when
    var userDetails = userDetailsService.loadUserByUsername("disableduser");

    // then
    assertThat(userDetails.isEnabled()).isFalse();
  }

  @Test
  @DisplayName("loadUserByUsername: 만료된 계정 로드")
  void shouldLoadUserDetailsForExpiredAccount() {
    // given
    var expiredUser =
        User.builder()
            .id(4L)
            .username("expireduser")
            .email("expired@example.com")
            .password("encodedPassword")
            .roles(List.of(Role.ROLE_USER))
            .enabled(true)
            .accountNonExpired(false)
            .accountNonLocked(true)
            .credentialsNonExpired(true)
            .build();

    given(userRepository.findByUsername("expireduser")).willReturn(Optional.of(expiredUser));

    // when
    var userDetails = userDetailsService.loadUserByUsername("expireduser");

    // then
    assertThat(userDetails.isAccountNonExpired()).isFalse();
  }
}
