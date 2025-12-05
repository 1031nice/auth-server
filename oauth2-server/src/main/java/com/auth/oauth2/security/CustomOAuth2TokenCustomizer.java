package com.auth.oauth2.security;

import com.auth.oauth2.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomOAuth2TokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

  private final UserRepository userRepository;

  @Override
  public void customize(JwtEncodingContext context) {
    Authentication principal = context.getPrincipal();

    if (principal != null && principal.getName() != null) {
      // Add user information to the token
      // principal.getName() actually returns the email
      userRepository
          .findByEmail(principal.getName())
          .ifPresent(
              user -> {
                List<String> roles =
                    user.getRoles().stream().map(Enum::name).collect(Collectors.toList());

                context
                    .getClaims()
                    .claim("userId", user.getId())
                    .claim("username", user.getUsername())
                    .claim("email", user.getEmail());

                // Add roles as scopes if not already present
                if (context.getAuthorizedScopes() != null
                    && !context.getAuthorizedScopes().isEmpty()) {
                  context.getClaims().claim("scope", context.getAuthorizedScopes());
                } else if (!roles.isEmpty()) {
                  context.getClaims().claim("scope", roles);
                }
              });
    }

    // Ensure token type is set
    context.getClaims().claim("token_type", OAuth2AccessToken.TokenType.BEARER.getValue());
  }
}
