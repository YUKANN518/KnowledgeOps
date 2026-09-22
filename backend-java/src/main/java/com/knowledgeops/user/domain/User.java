package com.knowledgeops.user.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "users")
public class User {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "department_id")
  private Department department;

  @Column(nullable = false, unique = true, length = 254)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private UserStatus status;

  @Version private long version;

  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles = new HashSet<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected User() {}

  public User(
      UUID id,
      Department department,
      String email,
      String passwordHash,
      String displayName,
      Set<Role> roles,
      Instant now) {
    this.id = id;
    this.department = department;
    this.email = email;
    this.passwordHash = passwordHash;
    this.displayName = displayName;
    this.roles = new HashSet<>(roles);
    this.status = UserStatus.ACTIVE;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public UUID getDepartmentId() {
    return department.getId();
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getDisplayName() {
    return displayName;
  }

  public UserStatus getStatus() {
    return status;
  }

  public long getVersion() {
    return version;
  }

  public Set<Role> getRoles() {
    return Set.copyOf(roles);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setStatus(UserStatus status, Instant now) {
    this.status = status;
    this.updatedAt = now;
  }

  public void replaceRoles(Set<Role> roles, Instant now) {
    this.roles.clear();
    this.roles.addAll(roles);
    this.updatedAt = now;
  }
}
