package com.auth.oauth2.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 100)
  private String email;

  @Column(nullable = false)
  private String password;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
  @Column(name = "role")
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private List<Role> roles = new ArrayList<>();

  @Column(nullable = false)
  @Builder.Default
  private Boolean enabled = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean accountNonExpired = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean accountNonLocked = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean credentialsNonExpired = true;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  /**
   * Returns the username used by Spring Security's UserDetailsService.
   * Actually returns the email address.
   */
  public String getUsername() {
    return email;
  }
}
