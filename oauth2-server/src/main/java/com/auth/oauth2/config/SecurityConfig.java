package com.auth.oauth2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  @Order(2)
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(
            csrf ->
                csrf
                    // OAuth2 엔드포인트는 state 파라미터로 CSRF 보호 (OAuth2 표준)
                    // 클라이언트 자격증명으로 보호되므로 CSRF 예외 처리
                    .ignoringRequestMatchers("/oauth2/**")
                    // API 엔드포인트는 JWT 토큰으로 보호되므로 CSRF 불필요
                    .ignoringRequestMatchers("/api/**"))
        // OAuth2 Authorization Code Flow를 위해 세션 사용
        .formLogin(
            form ->
                form.loginPage("/login")
                    .loginProcessingUrl("/login")
                    // false: saved request가 있으면 saved request로 리다이렉트 (OAuth2 flow)
                    // saved request가 없으면 defaultSuccessUrl로 리다이렉트
                    .defaultSuccessUrl("/", false)
                    .permitAll())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/login", "/login/**")
                    .permitAll()
                    .requestMatchers("/signup", "/signup/**")
                    .permitAll()
                    .requestMatchers("/api/v1/oauth2/clients/**")
                    .authenticated() // Client management requires authentication
                    .requestMatchers("/oauth2/**")
                    .permitAll()
                    .requestMatchers("/.well-known/**")
                    .permitAll()
                    .requestMatchers("/h2-console/**")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated());

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }
}
