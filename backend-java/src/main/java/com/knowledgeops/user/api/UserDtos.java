package com.knowledgeops.user.api;

import com.knowledgeops.user.domain.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class UserDtos {
  private UserDtos() {}

  public record Create(
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(max = 100) String displayName,
      @NotBlank @Size(min = 12, max = 72) String initialPassword,
      @NotEmpty Set<RoleCode> roles) {}

  public record StatusChange(@NotNull UserStatus status, @PositiveOrZero long expectedVersion) {}

  public record RoleChange(@NotEmpty Set<RoleCode> roles, @PositiveOrZero long expectedVersion) {}

  public record Response(
      UUID id,
      String email,
      String displayName,
      UserStatus status,
      Set<RoleCode> roles,
      Set<String> permissions,
      long version,
      Instant createdAt,
      Instant updatedAt) {
    public static Response from(User user) {
      Set<RoleCode> rs = new TreeSet<>(Comparator.comparing(Enum::name));
      Set<String> ps = new TreeSet<>();
      user.getRoles()
          .forEach(
              r -> {
                rs.add(r.getCode());
                r.getPermissions().forEach(p -> ps.add(p.getCode()));
              });
      return new Response(
          user.getId(),
          user.getEmail(),
          user.getDisplayName(),
          user.getStatus(),
          rs,
          ps,
          user.getVersion(),
          user.getCreatedAt(),
          user.getUpdatedAt());
    }
  }

  public record RoleResponse(RoleCode code, Set<String> permissions) {
    public static RoleResponse from(Role role) {
      Set<String> p = new TreeSet<>();
      role.getPermissions().forEach(x -> p.add(x.getCode()));
      return new RoleResponse(role.getCode(), p);
    }
  }
}
