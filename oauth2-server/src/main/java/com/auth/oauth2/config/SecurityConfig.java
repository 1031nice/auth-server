package com.auth.oauth2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  @Order(2)
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, UserDetailsService userDetailsService) throws Exception {
    // Configure DaoAuthenticationProvider for user authentication
    // Create BCryptPasswordEncoder directly instead of registering as bean to avoid bean conflicts
    BCryptPasswordEncoder userPasswordEncoder = new BCryptPasswordEncoder();
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(userPasswordEncoder);

    http.authenticationProvider(authProvider)
        .csrf(
            csrf ->
                csrf
                    // OAuth2 endpoints are CSRF-protected by state parameter (OAuth2 standard)
                    // CSRF exemption since endpoints are protected by client credentials
                    .ignoringRequestMatchers("/oauth2/**")
                    // API endpoints don't need CSRF protection as they're secured by JWT tokens
                    .ignoringRequestMatchers("/api/**"))
        // Use sessions for OAuth2 Authorization Code Flow
        .formLogin(
            form ->
                form.loginPage("/login")
                    .loginProcessingUrl("/login")
                    // false: redirect to saved request if exists (OAuth2 flow)
                    // otherwise redirect to defaultSuccessUrl
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
    // For OAuth2 client secret validation: compare in plain text per OAuth2 standard
    // Explicitly specified with @Qualifier in OAuth2AuthorizationServerConfig
    return NoOpPasswordEncoder.getInstance();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }
}
