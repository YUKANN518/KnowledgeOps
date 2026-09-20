package com.knowledgeops.user.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "permissions")
public class Permission {
  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 80)
  private String code;

  @Column(nullable = false)
  private String description;

  protected Permission() {}

  public UUID getId() {
    return id;
  }

  public String getCode() {
    return code;
  }
}
