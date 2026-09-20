package com.knowledgeops.user.infrastructure;

import com.knowledgeops.user.domain.*;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
class JpaUserRepositoryAdapter implements UserRepository {
  private final SpringDataUserRepository delegate;

  JpaUserRepositoryAdapter(SpringDataUserRepository delegate) {
    this.delegate = delegate;
  }

  public Optional<User> findByEmail(String email) {
    return delegate.findByEmailIgnoreCase(email);
  }

  public Optional<User> findById(UUID id) {
    return delegate.findById(id);
  }

  public boolean existsByEmail(String email) {
    return delegate.existsByEmailIgnoreCase(email);
  }

  public User save(User user) {
    return delegate.save(user);
  }
}
