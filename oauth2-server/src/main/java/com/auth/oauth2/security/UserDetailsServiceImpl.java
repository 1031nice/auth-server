package com.auth.oauth2.security;

import com.auth.oauth2.domain.entity.User;
import com.auth.oauth2.repository.UserRepository;
import java.util.Collection;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    // The username parameter is actually an email
    User user =
        userRepository
            .findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

    Collection<GrantedAuthority> authorities =
        user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.name()))
            .collect(Collectors.toList());

    return org.springframework.security.core.userdetails.User.builder()
        .username(user.getUsername())
        .password(user.getPassword())
        .authorities(authorities)
        .accountExpired(!user.getAccountNonExpired())
        .accountLocked(!user.getAccountNonLocked())
        .credentialsExpired(!user.getCredentialsNonExpired())
        .disabled(!user.getEnabled())
        .build();
  }
}
