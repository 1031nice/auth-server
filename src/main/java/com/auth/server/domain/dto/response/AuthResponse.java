package com.auth.server.domain.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

  private String accessToken;
  private String refreshToken;
  @Builder.Default private String tokenType = "Bearer";
  private Long id;
  private String username;
  private String email;
  private List<String> roles;
}
