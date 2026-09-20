package com.knowledgeops.user.domain;

import java.util.*;

public interface RoleRepository {
  Optional<Role> findByCode(RoleCode code);

  List<Role> findAll();
}
