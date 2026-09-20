package com.knowledgeops.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.knowledgeops.auth.application.JwtService;
import com.knowledgeops.auth.infrastructure.AuthProperties;
import com.knowledgeops.user.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtServiceTest {
  private final Clock clock = Clock.fixed(Instant.parse("2026-09-20T00:00:00Z"), ZoneOffset.UTC);
  private final AuthProperties props =
      new AuthProperties(
          "test-only-secret-at-least-thirty-two-bytes-long-123456",
          "knowledgeops-test",
          Duration.ofMinutes(10),
          Duration.ofDays(7),
          false,
          "http://localhost:5173");

  @Test
  void issuesRequiredClaimsAndValidatesSignature() {
    User user = mock(User.class);
    Role role = mock(Role.class);
    UUID id = UUID.randomUUID(), sid = UUID.randomUUID();
    when(user.getId()).thenReturn(id);
    when(user.getRoles()).thenReturn(Set.of(role));
    when(role.getCode()).thenReturn(RoleCode.EMPLOYEE);
    JwtService service = new JwtService(props, clock);
    var decoded = service.decode(service.createAccessToken(user, sid));
    assertThat(decoded.getSubject()).isEqualTo(id.toString());
    assertThat(decoded.getId()).isNotBlank();
    assertThat(decoded.getClaimAsString("sid")).isEqualTo(sid.toString());
    assertThat(decoded.getClaimAsStringList("roles")).containsExactly("EMPLOYEE");
    assertThat(decoded.getExpiresAt()).isEqualTo(clock.instant().plusSeconds(600));
  }

  @Test
  void rejectsTamperedToken() {
    User user = mock(User.class);
    when(user.getId()).thenReturn(UUID.randomUUID());
    when(user.getRoles()).thenReturn(Set.of());
    JwtService service = new JwtService(props, clock);
    String token = service.createAccessToken(user, UUID.randomUUID());
    String tampered = token.substring(0, token.length() - 2) + "aa";
    assertThatThrownBy(() -> service.decode(tampered)).isInstanceOf(JwtException.class);
  }
}
