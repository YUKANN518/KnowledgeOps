package com.knowledgeops.user.domain;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "roles")
public class Role {
  @Id private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, unique = true, length = 40)
  private RoleCode code;

  @Column(nullable = false, length = 80)
  private String name;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "role_permissions",
      joinColumns = @JoinColumn(name = "role_id"),
      inverseJoinColumns = @JoinColumn(name = "permission_id"))
  private Set<Permission> permissions = new HashSet<>();

  protected Role() {}

  public UUID getId() {
    return id;
  }

  public RoleCode getCode() {
    return code;
  }

  public Set<Permission> getPermissions() {
    return Set.copyOf(permissions);
  }
}
