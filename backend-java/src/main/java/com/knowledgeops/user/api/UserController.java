package com.knowledgeops.user.api;

import com.knowledgeops.audit.domain.AuditAction;
import com.knowledgeops.auth.application.AuthenticatedUser;
import com.knowledgeops.user.application.UserService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class UserController {
  private final UserService service;

  public UserController(UserService service) {
    this.service = service;
  }

  @GetMapping("/users/me")
  UserDtos.Response me(@AuthenticationPrincipal AuthenticatedUser actor) {
    return UserDtos.Response.from(service.get(actor.id()));
  }

  @PostMapping("/users")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('user.manage')")
  UserDtos.Response create(
      @Valid @RequestBody UserDtos.Create input, @AuthenticationPrincipal AuthenticatedUser actor) {
    return UserDtos.Response.from(
        service.create(
            input.email(),
            input.displayName(),
            input.initialPassword(),
            input.roles(),
            actor.id(),
            AuditAction.USER_CREATE));
  }

  @GetMapping("/users/{id}")
  @PreAuthorize("hasAuthority('user.read')")
  UserDtos.Response get(@PathVariable UUID id) {
    return UserDtos.Response.from(service.get(id));
  }

  @PatchMapping("/users/{id}/status")
  @PreAuthorize("hasAuthority('user.manage')")
  UserDtos.Response status(
      @PathVariable UUID id,
      @Valid @RequestBody UserDtos.StatusChange input,
      @AuthenticationPrincipal AuthenticatedUser actor) {
    return UserDtos.Response.from(
        service.changeStatus(id, input.status(), input.expectedVersion(), actor.id()));
  }

  @PutMapping("/users/{id}/roles")
  @PreAuthorize("hasAuthority('user.manage')")
  UserDtos.Response roles(
      @PathVariable UUID id,
      @Valid @RequestBody UserDtos.RoleChange input,
      @AuthenticationPrincipal AuthenticatedUser actor) {
    return UserDtos.Response.from(
        service.assignRoles(id, input.roles(), input.expectedVersion(), actor.id()));
  }

  @GetMapping("/roles")
  @PreAuthorize("hasAuthority('user.read')")
  List<UserDtos.RoleResponse> roles() {
    return service.allRoles().stream().map(UserDtos.RoleResponse::from).toList();
  }
}
