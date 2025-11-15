package com.auth.server.security;

import com.auth.server.repository.UserRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.oidc.authentication.OidcUserInfoAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OidcUserInfoService {

  private final UserRepository userRepository;

  /**
   * OIDC UserInfo를 생성하는 매퍼 함수
   * OAuth2AuthorizationServerConfig에서 이 메서드를 참조하여 설정합니다.
   */
  public OidcUserInfo loadUser(OidcUserInfoAuthenticationContext authenticationContext) {
    OidcUserInfoAuthenticationToken authentication = authenticationContext.getAuthentication();
    Object principalObj = authentication.getPrincipal();

    // Principal에서 사용자명 추출
    final String username;
    if (principalObj instanceof Authentication) {
      username = ((Authentication) principalObj).getName();
    } else {
      username = principalObj.toString();
    }

    // 데이터베이스에서 사용자 정보 조회
    return userRepository
        .findByUsername(username)
        .map(
            user -> {
              // OIDC 표준 클레임으로 사용자 정보 구성
              Map<String, Object> claims = new HashMap<>();
              claims.put("sub", user.getId().toString()); // Subject (사용자 식별자)
              claims.put("name", user.getUsername()); // 사용자 이름
              claims.put("email", user.getEmail()); // 이메일
              claims.put("email_verified", true); // 이메일 인증 여부 (기본값)
              claims.put("preferred_username", user.getUsername()); // 선호 사용자명

              return new OidcUserInfo(claims);
            })
        .orElseGet(
            () -> {
              // 사용자를 찾을 수 없는 경우, 기본값 반환
              Map<String, Object> claims = new HashMap<>();
              claims.put("sub", username != null ? username : "unknown");
              return new OidcUserInfo(claims);
            });
  }
}

