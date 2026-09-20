package com.knowledgeops.user.application;

import com.knowledgeops.audit.application.AuditService;
import com.knowledgeops.audit.domain.AuditAction;
import com.knowledgeops.auth.application.PasswordPolicy;
import com.knowledgeops.shared.domain.*;
import com.knowledgeops.user.domain.*;
import java.time.Clock;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private final UserRepository users;
  private final RoleRepository roles;
  private final DepartmentRepository departments;
  private final PasswordEncoder encoder;
  private final PasswordPolicy policy;
  private final AuditService audit;
  private final Clock clock;

  public UserService(
      UserRepository users,
      RoleRepository roles,
      DepartmentRepository departments,
      PasswordEncoder encoder,
      PasswordPolicy policy,
      AuditService audit,
      Clock clock) {
    this.users = users;
    this.roles = roles;
    this.departments = departments;
    this.encoder = encoder;
    this.policy = policy;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional
  public User create(
      String email,
      String displayName,
      String password,
      Set<RoleCode> requestedRoles,
      UUID actorId,
      AuditAction action) {
    String normalized = email.trim().toLowerCase(Locale.ROOT);
    policy.validate(password);
    if (users.existsByEmail(normalized))
      throw new BusinessException(
          ErrorCode.EMAIL_CONFLICT, HttpStatus.CONFLICT, "Email already exists");
    Department department =
        departments.findById(Department.GENERAL_ID).orElseThrow(BusinessException::notFound);
    Set<Role> resolved =
        resolve(requestedRoles.isEmpty() ? Set.of(RoleCode.EMPLOYEE) : requestedRoles);
    User user =
        users.save(
            new User(
                UUID.randomUUID(),
                department,
                normalized,
                encoder.encode(password),
                displayName.trim(),
                resolved,
                clock.instant()));
    audit.record(
        actorId == null ? "SYSTEM" : "USER",
        actorId,
        action,
        "USER",
        user.getId().toString(),
        Map.of(
            "email",
            normalized,
            "roles",
            requestedRoles.stream().map(Enum::name).sorted().toList()));
    return user;
  }

  @Transactional(readOnly = true)
  public User get(UUID id) {
    return users.findById(id).orElseThrow(BusinessException::notFound);
  }

  @Transactional
  public User changeStatus(UUID id, UserStatus status, long expectedVersion, UUID actorId) {
    User user = get(id);
    if (user.getVersion() != expectedVersion)
      throw new BusinessException(
          ErrorCode.STATE_CONFLICT, HttpStatus.CONFLICT, "User version changed");
    user.setStatus(status, clock.instant());
    audit.record(
        "USER",
        actorId,
        AuditAction.USER_DISABLE,
        "USER",
        id.toString(),
        Map.of("status", status.name()));
    return user;
  }

  @Transactional
  public User assignRoles(UUID id, Set<RoleCode> roleCodes, long expectedVersion, UUID actorId) {
    User user = get(id);
    if (user.getVersion() != expectedVersion)
      throw new BusinessException(
          ErrorCode.STATE_CONFLICT, HttpStatus.CONFLICT, "User version changed");
    user.replaceRoles(resolve(roleCodes), clock.instant());
    audit.record(
        "USER",
        actorId,
        AuditAction.ROLE_ASSIGN,
        "USER",
        id.toString(),
        Map.of("roles", roleCodes.stream().map(Enum::name).sorted().toList()));
    return user;
  }

  @Transactional(readOnly = true)
  public List<Role> allRoles() {
    return roles.findAll();
  }

  private Set<Role> resolve(Set<RoleCode> codes) {
    Set<Role> result = new HashSet<>();
    for (RoleCode code : codes)
      result.add(roles.findByCode(code).orElseThrow(BusinessException::notFound));
    return result;
  }
}
