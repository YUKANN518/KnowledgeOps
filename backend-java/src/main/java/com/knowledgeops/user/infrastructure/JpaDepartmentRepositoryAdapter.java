package com.knowledgeops.user.infrastructure;

import com.knowledgeops.user.domain.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

interface SpringDataDepartmentRepository extends JpaRepository<Department, UUID> {}

@Repository
class JpaDepartmentRepositoryAdapter implements DepartmentRepository {
  private final SpringDataDepartmentRepository delegate;

  JpaDepartmentRepositoryAdapter(SpringDataDepartmentRepository delegate) {
    this.delegate = delegate;
  }

  public Optional<Department> findById(UUID id) {
    return delegate.findById(id);
  }
}
