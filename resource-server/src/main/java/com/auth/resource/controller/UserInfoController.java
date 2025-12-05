package com.auth.resource.controller;

import com.auth.resource.security.OidcUserInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserInfoController {

  private final OidcUserInfoService oidcUserInfoService;

  /**
   * OIDC UserInfo endpoint.
   * Returns user information using the access token issued by the OAuth2 Authorization Server.
   */
  @GetMapping("/userinfo")
  public ResponseEntity<OidcUserInfo> getUserInfo(Authentication authentication) {
    OidcUserInfo userInfo = oidcUserInfoService.getUserInfo(authentication);
    return ResponseEntity.ok(userInfo);
  }
}

