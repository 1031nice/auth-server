package com.auth.resource.security;

import com.auth.resource.repository.UserRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OidcUserInfoService {

  private final UserRepository userRepository;

  /**
   * Extracts user information from JWT token and creates OIDC UserInfo.
   * Called by Resource Server, retrieves user information from JWT claims.
   */
  public OidcUserInfo getUserInfo(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
      return createDefaultUserInfo("unknown");
    }

    Jwt jwt = (Jwt) authentication.getPrincipal();
    
    String username = jwt.getClaimAsString("username");
    String userId = jwt.getClaimAsString("userId");
    String email = jwt.getClaimAsString("email");

    if (username != null || userId != null) {
      Map<String, Object> claims = new HashMap<>();
      claims.put("sub", userId != null ? userId : username);
      if (username != null) {
        claims.put("name", username);
        claims.put("preferred_username", username);
      }
      if (email != null) {
        claims.put("email", email);
        claims.put("email_verified", true);
      }
      return new OidcUserInfo(claims);
    }

    String subject = jwt.getSubject();
    if (subject != null) {
      return userRepository
          .findByUsername(subject)
          .map(
              user -> {
                Map<String, Object> claims = new HashMap<>();
                claims.put("sub", user.getId().toString());
                claims.put("name", user.getUsername());
                claims.put("email", user.getEmail());
                claims.put("email_verified", true);
                claims.put("preferred_username", user.getUsername());
                return new OidcUserInfo(claims);
              })
          .orElseGet(() -> createDefaultUserInfo(subject));
    }

    return createDefaultUserInfo("unknown");
  }

  private OidcUserInfo createDefaultUserInfo(String subject) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", subject != null ? subject : "unknown");
    return new OidcUserInfo(claims);
  }
}

