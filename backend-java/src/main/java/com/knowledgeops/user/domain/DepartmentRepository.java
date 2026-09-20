package com.knowledgeops.user.domain;

import java.util.*;

public interface DepartmentRepository {
  Optional<Department> findById(UUID id);
}
