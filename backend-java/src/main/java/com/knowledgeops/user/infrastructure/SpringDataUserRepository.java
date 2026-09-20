package com.knowledgeops.user.infrastructure;

import com.knowledgeops.user.domain.User;
import java.util.*;
import org.springframework.data.jpa.repository.*;

interface SpringDataUserRepository extends JpaRepository<User, UUID> {
  @EntityGraph(attributePaths = {"roles", "roles.permissions"})
  Optional<User> findByEmailIgnoreCase(String email);

  @Override
  @EntityGraph(attributePaths = {"roles", "roles.permissions"})
  Optional<User> findById(UUID id);

  boolean existsByEmailIgnoreCase(String email);
}
