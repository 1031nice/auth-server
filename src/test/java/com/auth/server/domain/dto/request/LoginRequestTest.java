package com.auth.server.domain.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LoginRequest validation tests")
class LoginRequestTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    var factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("유효성 검증 통과: 유효한 요청")
  void shouldPassValidationWhenRequestIsValid() {
    // given
    var request =
        LoginRequest.builder().username("testuser").password("password123").build();

    // when
    var violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("유효성 검증 실패: username이 null")
  void shouldFailValidationWhenUsernameIsNull() {
    // given
    var request = LoginRequest.builder().username(null).password("password123").build();

    // when
    var violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage()).isEqualTo("Username is required");
  }

  @Test
  @DisplayName("유효성 검증 실패: username이 빈 문자열")
  void shouldFailValidationWhenUsernameIsBlank() {
    // given
    var request = LoginRequest.builder().username("").password("password123").build();

    // when
    var violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage()).isEqualTo("Username is required");
  }

  @Test
  @DisplayName("유효성 검증 실패: password가 null")
  void shouldFailValidationWhenPasswordIsNull() {
    // given
    var request = LoginRequest.builder().username("testuser").password(null).build();

    // when
    var violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage()).isEqualTo("Password is required");
  }

  @Test
  @DisplayName("유효성 검증 실패: password가 빈 문자열")
  void shouldFailValidationWhenPasswordIsBlank() {
    // given
    var request = LoginRequest.builder().username("testuser").password("   ").build();

    // when
    var violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getMessage()).isEqualTo("Password is required");
  }

  @Test
  @DisplayName("유효성 검증 실패: username과 password 모두 null")
  void shouldFailValidationWhenBothUsernameAndPasswordAreNull() {
    // given
    var request = LoginRequest.builder().username(null).password(null).build();

    // when
    var violations = validator.validate(request);

    // then
    assertThat(violations).hasSize(2);
  }
}
