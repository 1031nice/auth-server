package com.auth.oauth2.repository;

import com.auth.oauth2.domain.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  // Method for Spring Security compatibility (actually queries by email)
  default Optional<User> findByUsername(String username) {
    return findByEmail(username);
  }
}
