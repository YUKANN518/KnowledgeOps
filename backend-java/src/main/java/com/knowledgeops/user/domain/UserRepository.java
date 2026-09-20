package com.knowledgeops.user.domain;

import java.util.*;

public interface UserRepository {
  Optional<User> findByEmail(String email);

  Optional<User> findById(UUID id);

  boolean existsByEmail(String email);

  User save(User user);
}
