package com.knowledgeops.user.infrastructure;

import com.knowledgeops.user.domain.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

interface SpringDataRoleRepository extends JpaRepository<Role, UUID> {
  Optional<Role> findByCode(RoleCode code);
}

@Repository
class JpaRoleRepositoryAdapter implements RoleRepository {
  private final SpringDataRoleRepository delegate;

  JpaRoleRepositoryAdapter(SpringDataRoleRepository delegate) {
    this.delegate = delegate;
  }

  public Optional<Role> findByCode(RoleCode code) {
    return delegate.findByCode(code);
  }

  public List<Role> findAll() {
    return delegate.findAll();
  }
}
